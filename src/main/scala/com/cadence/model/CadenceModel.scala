package com.cadence.model

import org.joda.time.format.{ISODateTimeFormat, DateTimeFormat}
import org.joda.time.{DateTimeZone, DateTime, LocalDate}
import spray.json._

/**
 * Created by aparrish on 7/29/14.
 */
case class CadenceUser(id: Option[Int],
                       email : String,
                       firstName : String,
                       lastName : String,
                       company : String,
                       password : String)

case class Metric( uuid : String, appid : String )

case class RawCheckin( uuid : String)

case class Checkin( id : Option[Int], uuid : String, date : DateTime = DateTime.now())

case class GraphMetric( count : Int, time : String )

object Dates {

  private val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")

  private val localDateFormat = ISODateTimeFormat.basicDate()

  def readDateTime(str : String) : DateTime = dateTimeFormat.parseDateTime(str)

  def write(dateTime : DateTime) : String = dateTimeFormat.print(dateTime)

  def readLocalDate(str : String) : LocalDate = localDateFormat.parseLocalDate(str)

  def write(localDate : LocalDate) : String = localDateFormat.print(localDate)

  def nowDTStr : String = write(nowDT)

  def nowDT : DateTime = DateTime.now(DateTimeZone.UTC)

  def nowLD : LocalDate = LocalDate.now(DateTimeZone.UTC)

  def nowLDStr : String = write(nowLD)

}

trait CadenceJsonProtocol extends DefaultJsonProtocol {


  implicit object JodaDateTimeFormat extends JsonFormat[DateTime] {

    def write(obj : DateTime) : JsValue = JsString(Dates.write(obj))

    def read(json : JsValue) : DateTime = json match {
      case JsString(x) => Dates.readDateTime(x)
      case _           => deserializationError("Expected String value for DateTime")
    }
  }

  implicit object JodaLocalDateFormat extends JsonFormat[LocalDate] {

    def write(obj : LocalDate) : JsValue = JsString(Dates.write(obj))

    def read(json : JsValue) : LocalDate = json match {
      case JsString(x) => Dates.readLocalDate(x)
      case _           => deserializationError("Expected String value for LocalDate")
    }
  }

  implicit val cadenceUser2json = jsonFormat6(CadenceUser)
  implicit val checkin2json = jsonFormat3(Checkin)
  implicit val rawCheckin2json = jsonFormat1(RawCheckin)
  implicit val metric2json = jsonFormat2(Metric)
  implicit val graphMetric2json = jsonFormat2(GraphMetric)

}

