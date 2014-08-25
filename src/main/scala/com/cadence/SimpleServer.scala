package com.cadence

import akka.actor._
import akka.io.IO
import com.cadence.framework.Logging
import com.cadence.model._
import com.cadence.model.repository._
import org.joda.time.DateTime
import org.omg.CosNaming.NamingContextPackage.NotFound
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket
import spray.can.websocket.frame.{Frame, BinaryFrame, TextFrame}
import spray.can.websocket.FrameCommandFailed
import spray.http.MediaTypes._
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing.HttpServiceActor
import scala.collection.Map
import scala.io.Source
import scala.util.Random
import akka.io.Tcp.{ConnectionClosed, PeerClosed}

/**
 * Created by aparrish on 8/23/14.
 */
object SimpleServer extends App with MySslConfiguration with Logging {
  var users = Map[String, User]()

  object WebSocketServer {
    def props() = Props(classOf[WebSocketServer])
  }

  class WebSocketServer extends Actor with ActorLogging {
    def receive = {
      // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
      case Http.Connected(remoteAddress, localAddress) =>
        val serverConnection = sender()
        val conn = context.actorOf(WebSocketWorker.props(serverConnection, self))
        log.debug("Connection added: {}", conn)
        serverConnection ! Http.Register(conn)
      case x: ForwardFrame =>
        context.children.map(_ ! x)
      case _ =>
      // consume and ignore
    }
  }

  object WebSocketWorker {
    def props(serverConnection: ActorRef, server: ActorRef) = Props(classOf[WebSocketWorker], serverConnection, server)
  }

  class WebSocketWorker(val serverConnection: ActorRef, val server: ActorRef)
    extends HttpServiceActor
    with SprayJsonSupport
    with websocket.WebSocketServerConnection
    with CadenceJsonProtocol {

    import RequestJsonProtocol._
    import com.cadence.UserProtocol._

    override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

    def businessLogic: Receive = {
      case x@(_: TextFrame) =>
        val obj = x.payload.utf8String.asJson.convertTo[Request]
        self ! obj
      case x@(_: ForwardFrame) =>
        send(x.frame)
      case x: WsCreate =>
        log.debug("Got WsCreate: {}", x)
        val newUsers = x.data.map { u : User =>
          val user = User(u.name, u.age, Some(u.id.getOrElse(Random.alphanumeric.take(5).mkString)))
          users += (user.id.get -> user)
          user
        }
        server ! ForwardFrame(TextFrame(WsCreate(newUsers).asInstanceOf[Request].toJson.compactPrint))
      case x: WsRead =>
        log.debug("Got WsRead: {}", x)
        send(TextFrame(JsObject("event" -> JsString("read"), "data" -> users.values.toJson).compactPrint))
      case x: WsUpdate =>
        log.debug("Got WsUpdate: {}", x)
        x.data foreach { u : User =>
          users += (u.id.get -> u)
        }
        server ! ForwardFrame(TextFrame(x.asInstanceOf[Request].toJson.compactPrint))
      case x: WsDelete =>
        log.debug("Got WsDelete: {}", x)
        x.data foreach { u =>
          users -= u.id.get
        }
        server ! ForwardFrame(TextFrame(x.asInstanceOf[Request].toJson.compactPrint))

      case websocket.UpgradedToWebSocket =>
        self ! WsRead(Data(1, 25, 0, None))
      case x: FrameCommandFailed =>
        log.error("frame command failed {}", x)
      case x: ConnectionClosed =>
        context.stop(self)
      case x@(_: Any) =>
        log.warning("ignored message : {}", x)
    }

    def businessLogicNoUpgrade: Receive = {
      implicit val refFactory: ActorRefFactory = context
      runRoute( userRoutes ~ appRoutes ~ metricRoutes)
    }

    /** ****************
      * Static Asset Endpoints
      */

    def indexPath = pathEndOrSingleSlash {
      getFromResource("index.html")
    }

    def indexFile = path("index.html") {
      getFromResource("index.html")
    }

    def lib = pathPrefix("lib") {
      getFromResourceDirectory("lib")
    }

    def app = pathPrefix("app") {
      getFromResourceDirectory("app")
    }

    /** *****************
      * User Endpoints
      */
    val userEndpointPrefix = "users"

    def registerUser = {
      implicit val cadenceUserRegistrationRequest2json = jsonFormat5(CadenceRegistrationRequest)

      pathPrefix(userEndpointPrefix / "register") {
        post {
          entity(as[CadenceRegistrationRequest]) { registrationRequest =>
            respondWithMediaType(`application/json`) {
              debug("Registering a user")

              complete {
                addUser(
                  CadenceUser(
                    None,
                    registrationRequest.firstName,
                    registrationRequest.lastName,
                    registrationRequest.email,
                    registrationRequest.company,
                    registrationRequest.password) )

                StatusCodes.OK
              }
            }
          }
        }
      }
    }

    def loginUser = {
      implicit val cadenceLoginRequest2json = jsonFormat2(CadenceLoginRequest)
      implicit val cadenceUser2json = jsonFormat6(CadenceUser)

      pathPrefix(userEndpointPrefix / "login" ) {
        post {
          entity(as[CadenceLoginRequest]) { loginRequest =>
            respondWithMediaType(`application/json`) {
              complete {
                val userByEmail = findByEmail(loginRequest.email)
                userByEmail match {
                  case None => StatusCodes.Forbidden
                  case Some(u) => {
                    if( u.password == loginRequest.password ) {
                      u
                    }
                    else
                    {
                      StatusCodes.Forbidden
                    }
                  }
                }
              }
            }
          }
        }
      }

    }

    val userRoutes = registerUser ~ loginUser


    /** *******************
      * App Endpoints
      */
    val appsEndpointPrefix = "apps"

    def addApplicationToUser = {
      implicit val cadenceAddApplicationRequest2json = jsonFormat5(CadenceAddApplicationRequest)
      implicit val cadenceApplication2json = jsonFormat6(Application)

      pathPrefix(appsEndpointPrefix) {
        post {
          entity(as[CadenceAddApplicationRequest]) {
            addRequest =>
              respondWithMediaType(`application/json`) {
                complete {

                  val appToAdd = Application(
                    None,
                    addRequest.name,
                    addRequest.market,
                    addRequest.url,
                    java.util.UUID.randomUUID().toString,
                    addRequest.appType )

                  val appId = addApplication(appToAdd)

                  val appOwnerId = addApplicationOwner(ApplicationOwner(None, addRequest.ownerId, appId))

                  appToAdd.copy(id = Some(appId))
                }
              }
          }
        }
      }

    }

    def findAllAppsForUser = {
      implicit val applications2json = jsonFormat6(Application)

      implicit object ApplicationListTypeFormat extends JsonFormat[List[Application]] {
        override def write(obj : List[Application]) : JsValue = JsArray(obj.map(applications2json.write))

        override def read(json : JsValue) : List[Application] = json match {
          case JsArray(x) => x.map(applications2json.read)
          case _          => deserializationError("Expected String value for List[Application]")
        }
      }

      pathPrefix(appsEndpointPrefix) {
        parameters('userId.as[Int]) { userId =>
          get {
            respondWithMediaType(`application/json`) {
              complete {
                findApplicationsForOwner(userId)
              }
            }
          }
        }
      }
    }

    def findOneAppById = {
      implicit val applications2json = jsonFormat6(Application)

      pathPrefix(appsEndpointPrefix / IntNumber ) { appId =>

          debug(s"Finding an application by id: $appId")
          get {
            respondWithMediaType(`application/json`) {
              complete {
                findApplicationById(appId)
              }
            }
          }

      }
    }

    def activateDevice = {
      pathPrefix(appsEndpointPrefix / "activate" / JavaUUID ) { deviceId =>
        get {
          debug("Generating an activate id for device: " + deviceId)

          respondWithMediaType(`application/json`) {
            complete( java.util.UUID.randomUUID().toString )
          }
        }
      }
    }

    val appRoutes = addApplicationToUser ~ findOneAppById ~ findAllAppsForUser ~ activateDevice

    def checkinFromDevice = {
      implicit val checkin2json = jsonFormat2(MetricRequest)

      pathPrefix("metrics" / "checkin" ) {
        post {
          entity(as[MetricRequest]) {
            metric => {
              respondWithMediaType(`application/json`) {
                complete{
                  insertMetric(Metric(None, metric.appId, metric.deviceId))

                  import RequestJsonProtocol._
                  val update : WsMetricsChange = WsMetricsChange()
                  server ! ForwardFrame(TextFrame(update.asInstanceOf[Request].toJson.compactPrint))

                  StatusCodes.OK
                }
              }
            }
          }
        }
      }
    }


    def rawMetrics = {
      implicit val metric2json = jsonFormat4(Metric)


      implicit object MetricListTypeFormat extends JsonFormat[List[Metric]] {
        override def write(obj : List[Metric]) : JsValue = JsArray(obj.map(metric2json.write))

        override def read(json : JsValue) : List[Metric] = json match {
          case JsArray(x) => x.map(metric2json.read)
          case _          => deserializationError("Expected String value for List[Metric]")
        }
      }

      pathPrefix("metrics" / "raw" / IntNumber) { appId =>
        get {
          respondWithMediaType(`application/json`) {
            complete{
              listMetrics()
            }
          }
        }
      }
    }

    def graphMetrics = {
      implicit val graphMetric2json = jsonFormat3(GraphMetricResult)


      implicit object GraphMetricResultListTypeFormat extends JsonFormat[List[GraphMetricResult]] {
        override def write(obj : List[GraphMetricResult]) : JsValue = JsArray(obj.map(graphMetric2json.write))

        override def read(json : JsValue) : List[GraphMetricResult] = json match {
          case JsArray(x) => x.map(graphMetric2json.read)
          case _          => deserializationError("Expected String value for List[GraphMetricResult]")
        }
      }

      pathPrefix("metrics" / IntNumber ) { appId =>
        get {
          respondWithMediaType(`application/json`) {
            complete{
              val graphMetrics = graphCheckins("")
              val results : List[GraphMetricResult] = graphMetrics.map { gr => GraphMetricResult(
                gr.count + gr.time, gr.count, gr.time
              )}
              results
            }
          }
        }
      }
    }

    val metricRoutes = checkinFromDevice ~ rawMetrics ~ graphMetrics

  }




  def responseFrame(eventName: String, user: User): TextFrame = {
    import com.cadence.UserProtocol._
    TextFrame(JsObject("event" -> JsString(eventName), "data" -> user.toJson).compactPrint)
  }

  class DeadLetterListener extends Actor {
    def receive = {
      case d: DeadLetter => println(d)
    }
  }

  def doMain() {

    implicit val system = ActorSystem()

    val server = system.actorOf(WebSocketServer.props(), "websocket")
    val listener = system.actorOf(Props(classOf[DeadLetterListener]))
    system.eventStream.subscribe(listener, classOf[DeadLetter])

    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)

  }

  doMain()
}
