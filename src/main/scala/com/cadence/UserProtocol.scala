package com.cadence

import spray.json.{DefaultJsonProtocol, JsonFormat}

/**
 * Created by aparrish on 8/23/14.
 */
object UserProtocol extends DefaultJsonProtocol {
  implicit val userFormat = jsonFormat3(User)
  implicit val sortFormat : JsonFormat[Sort] = jsonFormat2(Sort)
  implicit val dataFormat : JsonFormat[Data] = jsonFormat4(Data)
}
