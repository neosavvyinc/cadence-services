package com.cadence.service

import akka.actor.{ Actor, Props, ActorLogging, ActorRef, ActorRefFactory }
import com.cadence.model.{CadenceJsonProtocol, Metric, Checkin, CadenceUser}
import com.cadence.model.repository._
import org.joda.time.DateTime
import spray.can.Http
import spray.can.websocket
import spray.can.websocket.frame.{TextFrame, BinaryFrame}
import spray.http.StatusCodes._
import spray.http.{StatusCodes, HttpRequest}
import spray.can.websocket.{FrameCommand, FrameCommandFailed}
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport
import spray.routing.HttpServiceActor

import scala.collection.Map

/**
 * Created by aparrish on 7/31/14.
 */

final case class Push(msg: String)

object WebSocketServer {
  def props() = Props(classOf[WebSocketServer])
}
class WebSocketServer extends Actor with ActorLogging {


  def receive = {
    // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val conn = context.actorOf(WebSocketWorker.props(serverConnection, self))
      serverConnection ! Http.Register(conn)
  }
}

object WebSocketWorker {
  def props(serverConnection: ActorRef, server : ActorRef) = Props(classOf[WebSocketWorker], serverConnection, server)
}
class WebSocketWorker(val serverConnection: ActorRef, val server : ActorRef) extends HttpServiceActor
                                                      with websocket.WebSocketServerConnection
                                                      with SprayJsonSupport
                                                      with CadenceJsonProtocol {
  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  def businessLogic: Receive = {
    // just bounce frames back for Autobahn testsuite
    case x @ (_: BinaryFrame | _: TextFrame ) =>
      sender() ! x

    case Push(msg) => send(TextFrame(msg))

    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case x: HttpRequest => {
      println(x)
    }
  }

  def businessLogicNoUpgrade: Receive = {
    implicit val refFactory: ActorRefFactory = context
    runRoute(cadenceRoute)
  }

//  def setup = pathPrefix("setup") {
//    post{
//      respondWithMediaType(`application/json`)
//      entity(as[String]) {
//        emailAddress =>
//          complete {
//            def uuid = java.util.UUID.randomUUID.toString
//            val u = CadenceUser(None, emailAddress, uuid)
//            addUser(u)
//
//            println("Trying to send a notification to the client to reload")
//            server ! FrameCommand(TextFrame("reloadUsers"))
//
//            u
//          }
//      }
//    }
//  }

  def showUsers = pathPrefix("listUsers") {
    get {
      respondWithMediaType(`application/json`)
      complete {
        listUsers()
      }

    }
  }

  def checkin = pathPrefix("checkin") {
    post {
      respondWithMediaType(`application/json`)
      entity(as[Checkin]) {
        checkin =>
          complete {
            StatusCodes.OK
          }
      }
    }
  }

  def showMetrics = pathPrefix("metrics") {
    get {
      respondWithMediaType(`application/json`)
      complete {


        StatusCodes.OK
      }

    }
  }

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


  val cadenceRoute =
//    setup ~
      showUsers ~
      checkin ~
      showMetrics ~
      lib ~
      app ~
      indexPath ~
      indexFile ~
      complete(NotFound)
}
