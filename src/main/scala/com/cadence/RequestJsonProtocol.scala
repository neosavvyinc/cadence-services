package com.cadence

import com.cadence.model.{CadenceJsonProtocol, CadenceRegistrationRequest, CadenceUser}
import spray.json._

/**
 * Created by aparrish on 8/23/14.
 */
object RequestJsonProtocol extends CadenceJsonProtocol {
  import com.cadence.UserProtocol._


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
