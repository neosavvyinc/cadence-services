/*
 * Copyright 2014 Elastic Modules Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import akka.actor._
import akka.io.IO
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

case class Sort(property: String, direction: String)
case class Data(page: Int, limit: Int, start: Int, sort: Option[Array[Sort]])
case class User(name: String, age: Int, id: Option[String])

trait Request
case class WsDataChange() extends Request
case class WsMetricsChange() extends Request
case class WsCreate(data: List[User]) extends Request
case class WsRead(data: Data) extends Request
case class WsUpdate(data: List[User]) extends Request
case class WsDelete(data: List[User]) extends Request

case class ForwardFrame(frame: Frame)

object UserProtocol extends DefaultJsonProtocol {
  implicit val userFormat = jsonFormat3(User)
  implicit val sortFormat : JsonFormat[Sort] = jsonFormat2(Sort)
  implicit val dataFormat : JsonFormat[Data] = jsonFormat4(Data)
}

object RequestJsonProtocol extends CadenceJsonProtocol {
  import UserProtocol._


  implicit val cadenceUser2json = jsonFormat6(CadenceUser)
  implicit val cadenceUserRegistrationRequest2json = jsonFormat5(CadenceRegistrationRequest)



  implicit object RequestJsonFormat extends RootJsonFormat[Request] {
    override def write(obj: Request): JsValue = obj match {
      case p@(_: WsDataChange) =>
        JsObject("event" -> JsString("dataChanged") )
      case p@(_: WsMetricsChange) =>
        JsObject("event" -> JsString("metricsChanged") )
      case p@(_: WsCreate) =>
        JsObject("event" -> JsString("create"),"data" -> p.data.toJson)
      case p@(_: WsRead) =>
        JsObject("event" -> JsString("read"), "data" -> p.data.toJson)
      case p@(_: WsUpdate) =>
        JsObject("event" -> JsString("update"), "data" -> p.data.toJson)
      case p@(_: WsDelete) =>
        JsObject("event" -> JsString("destroy"), "data" -> p.data.toJson)
    }

    def read(value: JsValue) = value match {
      case JsObject(fields) =>
        (fields("event"), fields("data")) match {
          case (JsString("dataChanged"), _) => WsDataChange()
          case (JsString("metricsChanged"), _) => WsMetricsChange()
          case (JsString("create"), element) => WsCreate(element.convertTo[List[User]])
          case (JsString("read"), element) => WsRead(element.convertTo[Data])
          case (JsString("update"), element) => WsUpdate(element.convertTo[List[User]])
          case (JsString("destroy"), element) => WsDelete(element.convertTo[List[User]])
          case x@_ => throw new DeserializationException("Unhandled Request: " + x)
        }
      case _ => throw new DeserializationException("Request Expected")
    }
  }
}

object SimpleServer extends App with MySslConfiguration {
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
    import UserProtocol.userFormat
    import DefaultJsonProtocol._

    override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

    def businessLogic: Receive = {
      case x@(_: TextFrame) =>
        import RequestJsonProtocol._
        val obj = x.payload.utf8String.asJson.convertTo[Request]
        self ! obj
      case x@(_: ForwardFrame) =>
        send(x.frame)
      case x: WsCreate =>
        import RequestJsonProtocol._
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
        import RequestJsonProtocol._
        log.debug("Got WsUpdate: {}", x)
        x.data foreach { u : User =>
          users += (u.id.get -> u)
        }
        server ! ForwardFrame(TextFrame(x.asInstanceOf[Request].toJson.compactPrint))
      case x: WsDelete =>
        import RequestJsonProtocol._
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
      runRoute( userRoutes )
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

    def helloFromUser = {
      pathPrefix(userEndpointPrefix / "hello" ) {
        get {
          respondWithMediaType(`application/json`) {
            complete("Hello World!")
          }
        }
      }
    }

    def registerUser = {
      implicit val cadenceUserRegistrationRequest2json = jsonFormat5(CadenceRegistrationRequest)

      pathPrefix(userEndpointPrefix / "register") {
        post {
          entity(as[CadenceRegistrationRequest]) { registrationRequest =>
            respondWithMediaType(`application/json`) {
              complete {
                addUser(
                  CadenceUser(
                    None,
                    registrationRequest.email,
                    registrationRequest.firstName,
                    registrationRequest.lastName,
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

    val userRoutes = helloFromUser ~ registerUser ~ loginUser


    /** *******************
      * App Endpoints
      */


  }

  def responseFrame(eventName: String, user: User): TextFrame = {
    import UserProtocol.userFormat
    TextFrame(JsObject("event" -> JsString(eventName), "data" -> user.toJson).compactPrint)
  }

  class DeadLetterListener extends Actor {
    def receive = {
      case d: DeadLetter => println(d)
    }
  }

  def doMain() {
    import UserProtocol.userFormat
    import DefaultJsonProtocol._

    implicit val system = ActorSystem()

    val server = system.actorOf(WebSocketServer.props(), "websocket")
    val listener = system.actorOf(Props(classOf[DeadLetterListener]))
    system.eventStream.subscribe(listener, classOf[DeadLetter])

    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)

  }

  doMain()
}



