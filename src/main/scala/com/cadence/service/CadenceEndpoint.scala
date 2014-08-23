package com.cadence.service

import akka.actor.ActorSystem
import com.cadence.model.{Metric, Checkin, CadenceJsonProtocol, CadenceUser}
import org.joda.time.DateTime
import spray.http.MediaTypes._
import spray.http.StatusCodes
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.routing.HttpService
import com.cadence.model.repository._

import scala.collection._

/**
 * Created by aparrish on 7/29/14.
 */
trait CadenceEndpoint extends HttpService
     with SprayJsonSupport with CadenceJsonProtocol {


  def setup = pathPrefix("setup") {
    post{
      respondWithMediaType(`application/json`)
      entity(as[String]) {
        emailAddress =>
          complete {
            def uuid = java.util.UUID.randomUUID.toString
            val u = CadenceUser(None, emailAddress, uuid)
            addUser(u)



            u
          }
      }
    }
  }

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

            insertCheckin(Checkin(checkin.id, checkin.uuid))

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
    setup ~
    showUsers ~
    checkin ~
    showMetrics ~
    lib ~
    app ~
    indexPath ~
    indexFile ~
    complete(NotFound)
}
