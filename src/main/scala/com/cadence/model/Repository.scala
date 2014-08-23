package com.cadence.model

import org.joda.time.{LocalDate, DateTime}

import scala.slick.direct.AnnotationMapper.column
import scala.slick.lifted.ProvenShape

/**
 * Created by aparrish on 7/30/14.
 */
package object repository {

  import scala.slick.driver.MySQLDriver.simple._
  import com.github.tototoshi.slick.MySQLJodaSupport._
  import scala.slick.jdbc.{GetResult, StaticQuery => Q}


  object DBConfiguration {
    val statementCacheSize = 50
    val minConnectionsPerPartition = 100
    val maxConnectionsPerPartition = 100
    val numPartitions = 1

    val driver = "com.mysql.jdbc.Driver"
    val url = "jdbc:mysql://localhost/cadence"
    val user = "cadence"
    val pass = "cadence"
  }

  val db = Database.forURL( DBConfiguration.url,
    driver = DBConfiguration.driver,
    user = DBConfiguration.user,
    password = DBConfiguration.pass)

  val cadenceUsers = TableQuery[CadenceUserTable]
  val checkins = TableQuery[CheckinTable]



  class CadenceUserTable(tag: Tag) extends Table[CadenceUser](tag, "cadence_user") {

    def id: Column[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def email: Column[String] = column[String]("email")
    def uuid: Column[String] = column[String]("uuid")

    def * = (id.?, email, uuid) <> (CadenceUser.tupled, CadenceUser.unapply)

  }

  class CheckinTable(tag : Tag ) extends Table[Checkin](tag, "cadence_checkin") {

    import com.github.tototoshi.slick.MySQLJodaSupport._

    def id    = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def uuid  = column[String]("metric_user_uuid")
    def date  = column[DateTime]("metric_date")

    def * = (id.?, uuid, date) <> (Checkin.tupled, Checkin.unapply)


  }

  def dropAll() : Int = {

    db.withSession{
      implicit s => cadenceUsers.delete
    }
  }

  def addUser( user : CadenceUser ) : Int = {

    db.withSession{
      implicit session =>
        cadenceUsers.insert(user)
    }

  }

  def listUsers( ) : List[CadenceUser] = {

    db.withSession{
      implicit s => cadenceUsers.list
    }

  }

  def insertCheckin( checkin : Checkin ) : Int = {

    db.withSession {
      implicit session => checkins.insert( checkin )
    }

  }

  def listCheckins() : List[Checkin] = {
    db.withSession {
      implicit session => checkins.list
    }
  }

  /**
   * Need to improve these queries to pad for empty results:
   * http://stackoverflow.com/questions/11274335/grouping-records-hour-by-hour-or-day-by-day-and-filling-gaps-with-zero-or-null-i
   * http://www.artfulsoftware.com/infotree/qrytip.php?id=95
   * http://stackoverflow.com/questions/5988179/mysql-group-by-date-how-to-return-results-when-no-rows
   */

  implicit val getUserResult = GetResult(r => GraphMetric(r.<<, r.<<))
  val groupingQueryByMinute = Q[Unit, GraphMetric] + """select count(*) as count, DATE_FORMAT(metric_date, '%Y-%m-%d %H:%i:00 %Z') as time from cadence_checkin group by time"""
  val groupingQueryByHour = Q[Unit, GraphMetric] + """select count(*) as count, DATE_FORMAT(metric_date, '%Y-%m-%d %H:00:00 %Z') as time from cadence_checkin group by time"""
  val groupingQueryByDay = Q[Unit, GraphMetric] + """select count(*) as count, DATE_FORMAT(metric_date, '%Y-%m-%d') as time from cadence_checkin group by time"""
  val groupingQueryByMonth = Q[Unit, GraphMetric] + """select count(*) as count, DATE_FORMAT(metric_date, '%Y-%m') as time from cadence_checkin group by time"""

  def graphCheckins( groupingType : String ) : List[GraphMetric] = {

    groupingType match {
      case "Minute" => db.withSession {
        implicit session => {
          groupingQueryByMinute().list
        }
      }
      case "Hour" => db.withSession {
        implicit session => {
          groupingQueryByHour().list
        }
      }
      case "Day" => db.withSession {
        implicit session => {
          groupingQueryByDay().list
        }
      }
      case "Month" | _ => db.withSession {
        implicit session => {
          groupingQueryByMonth().list
        }
      }
    }

  }

}
