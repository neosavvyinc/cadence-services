package com.cadence.model

import com.cadence.framework.Logging
import org.joda.time.{LocalDate, DateTime}

import scala.slick.direct.AnnotationMapper.column
import scala.slick.lifted.ProvenShape

/**
 * Created by aparrish on 7/30/14.
 */
package object repository extends Logging {

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

  /**
   * USERS
   *
   * +------------+---------------+------+-----+---------+----------------+
   * | Field      | Type          | Null | Key | Default | Extra          |
   * +------------+---------------+------+-----+---------+----------------+
   * | ID         | int(11)       | NO   | PRI | NULL    | auto_increment |
   * | FIRST_NAME | varchar(1024) | NO   |     | NULL    |                |
   * | LAST_NAME  | varchar(1024) | NO   |     | NULL    |                |
   * | EMAIL      | varchar(1024) | NO   |     | NULL    |                |
   * +------------+---------------+------+-----+---------+----------------+
   */
  class CadenceUserTable(tag: Tag) extends Table[CadenceUser](tag, "USERS") {

    def id: Column[Int] = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def firstName : Column[String] = column[String]("FIRST_NAME")
    def lastName : Column[String] = column[String]("LAST_NAME")
    def email: Column[String] = column[String]("EMAIL")
    def company: Column[String] = column[String]("COMPANY")
    def password: Column[String] = column[String]("PASSWORD")

    def * = (id.?, firstName, lastName, email, company, password) <> (CadenceUser.tupled, CadenceUser.unapply)
  }
  val cadenceUsers = TableQuery[CadenceUserTable]
  object cadenceUsersExt extends TableQuery(new CadenceUserTable(_)) {
    // put extra methods here, e.g.:
    val findByID = this.findBy(_.id)
  }



  def dropAll() : Int = {

    db.withSession{
      implicit s =>
        cadenceUsers.delete
        applications.delete
        applicationOwners.delete
    }

  }

  def addUser( user : CadenceUser ) : Int = {

    db.withSession{
      implicit session => (cadenceUsers returning cadenceUsers.map(_.id)) += user
    }

  }

  def listUsers( ) : List[CadenceUser] = {

    db.withSession{
      implicit s => cadenceUsers.list
    }

  }

  def updateUser( user : CadenceUser ) : Int = {

    db.withSession {
      implicit s => cadenceUsers.update(user)
    }

  }

  def findById( id : Int ) : Option[CadenceUser] = {
    db.withSession{
      implicit s => cadenceUsersExt.findByID(id).firstOption()
    }
  }

  def findByEmail( email : String ) : Option[CadenceUser] = {
    db.withSession{
      implicit s =>
        val q = cadenceUsers.filter( _.email === email )
        debug("This is the debug statement: " + q.selectStatement)
        q.firstOption
    }
  }

  /**
   * APP OWNERS
   * +----------+------------+------+-----+---------+----------------+
   * | Field    | Type       | Null | Key | Default | Extra          |
   * +----------+------------+------+-----+---------+----------------+
   * | ID       | int(11)    | NO   | PRI | NULL    | auto_increment |
   * | OWNER_ID | bigint(20) | NO   |     | NULL    |                |
   * | APP_ID   | bigint(20) | NO   |     | NULL    |                |
   * +----------+------------+------+-----+---------+----------------+
   */
  class ApplicationOwnerTable(tag: Tag) extends Table[ApplicationOwner](tag, "APP_OWNERS") {
    def id: Column[Int] = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def ownerId : Column[Int] = column[Int]("OWNER_ID")
    def applicationId : Column[Int] = column[Int]("APP_ID")

    def * = (id.?, ownerId, applicationId) <> (ApplicationOwner.tupled, ApplicationOwner.unapply)
  }
//  val applicationOwners = TableQuery[ApplicationOwnerTable]
  object applicationOwners extends TableQuery(new ApplicationOwnerTable(_)) {
    // put extra methods here, e.g.:
    val findByOwnerId = this.findBy(_.ownerId)
  }

  def addApplicationOwner( applicationOwner : ApplicationOwner ) : Int = {
    db.withSession{
      implicit session => (applicationOwners returning applicationOwners.map(_.id)) += applicationOwner
    }
  }




  /**
   * APPS
   * +---------+---------------+------+-----+---------+----------------+
   * | Field   | Type          | Null | Key | Default | Extra          |
   * +---------+---------------+------+-----+---------+----------------+
   * | ID      | int(11)       | NO   | PRI | NULL    | auto_increment |
   * | NAME    | varchar(1024) | NO   |     | NULL    |                |
   * | MARKET  | varchar(1024) | YES  |     | NULL    |                |
   * | URL     | varchar(4096) | YES  |     | NULL    |                |
   * | API_KEY | varchar(1024) | NO   |     | NULL    |                |
   * | TYPE    | varchar(256)  | NO   |     | NULL    |                |
   * +---------+---------------+------+-----+---------+----------------+
   */
  class ApplicationTable(tag : Tag) extends Table[Application](tag, "APPS") {
    def id: Column[Int] = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name : Column[String] = column[String]("NAME")
    def market : Column[String] = column[String]("MARKET")
    def url: Column[String] = column[String]("URL")
    def apiKey: Column[String] = column[String]("API_KEY")
    def appType: Column[String] = column[String]("TYPE")

    def * = (id.?, name, market, url, apiKey, appType ) <> (Application.tupled, Application.unapply)
  }
  object applications extends TableQuery(new ApplicationTable(_)) {
    val findById = this.findBy(_.id)
  }

  def findApplicationsForOwner( ownerId : Int ) : List[Application] = {

    db.withSession {
      implicit session =>
        val applicationReferences = applicationOwners.findByOwnerId(ownerId).list
        val results = applicationReferences map {
          ar => applications.findById(ar.applicationId).first
        }

        results
    }

  }

  def findApplicationById( appId : Int ) : Option[Application] = {
    db.withSession{
      implicit session => applications.findById(appId).firstOption
    }
  }

  def addApplication( application : Application ) : Int = {
    db.withSession{
      implicit session => (applications returning applications.map(_.id)) += application
    }
  }

  def listApplications( ) : List[Application] = {

    db.withSession{
      implicit s => applications.list
    }

  }

  /**
   * SESSIONS
   * +-----------------+------------+------+-----+---------------------+-----------------------------+
   * | Field           | Type       | Null | Key | Default             | Extra                       |
   * +-----------------+------------+------+-----+---------------------+-----------------------------+
   * | SESSIONID       | char(36)   | NO   | PRI | NULL                |                             |
   * | USERID          | bigint(20) | NO   |     | NULL                |                             |
   * | CREATED         | timestamp  | NO   |     | CURRENT_TIMESTAMP   | on update CURRENT_TIMESTAMP |
   * | LASTACCESSED    | timestamp  | NO   |     | 0000-00-00 00:00:00 |                             |
   * | SESSION_INVALID | tinyint(1) | NO   |     | 1                   |                             |
   * +-----------------+------------+------+-----+---------------------+-----------------------------+
   */




  class MetricTable(tag : Tag ) extends Table[Metric](tag, "METRICS") {

    import com.github.tototoshi.slick.MySQLJodaSupport._

    def id    = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def appId  = column[String]("API_KEY")
    def deviceId  = column[String]("DEVICE_ID")
    def date  = column[DateTime]("METRIC_DATE")

    def * = (id.?, appId, deviceId, date) <> (Metric.tupled, Metric.unapply)
  }
  object metrics extends TableQuery(new MetricTable(_)) {
  }

  def insertMetric( metric : Metric ) : Int = {

    db.withSession {
      implicit session => (metrics returning metrics.map(_.id)) += metric
    }

  }

  def listMetrics() : List[Metric] = {
    db.withSession {
      implicit session => metrics.list
    }
  }

  /**
   * Need to improve these queries to pad for empty results:
   * http://stackoverflow.com/questions/11274335/grouping-records-hour-by-hour-or-day-by-day-and-filling-gaps-with-zero-or-null-i
   * http://www.artfulsoftware.com/infotree/qrytip.php?id=95
   * http://stackoverflow.com/questions/5988179/mysql-group-by-date-how-to-return-results-when-no-rows
   */

  implicit val getUserResult = GetResult(r => GraphMetric(r.<<, r.<<))
  val groupingQueryByMinute = Q[Unit, GraphMetric] + """select count(*) as count, DATE_FORMAT(metric_date, '%Y-%m-%d %H:%i:00 %Z') as time from METRICS group by time"""
  val groupingQueryByHour = Q[Unit, GraphMetric] + """select count(*) as count, DATE_FORMAT(metric_date, '%Y-%m-%d %H:00:00 %Z') as time from METRICS group by time"""
  val groupingQueryByDay = Q[Unit, GraphMetric] + """select count(*) as count, DATE_FORMAT(metric_date, '%Y-%m-%d') as time from METRICS group by time"""
  val groupingQueryByMonth = Q[Unit, GraphMetric] + """select count(*) as count, DATE_FORMAT(metric_date, '%Y-%m') as time from METRICS group by time"""

  def graphCheckins( groupingType : String ) : List[GraphMetric] = {

    groupingType match {

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
      case "Month" => db.withSession {
        implicit session => {
          groupingQueryByMonth().list
        }
      }
      case "Minute" | _ => db.withSession {
        implicit session => {
          groupingQueryByMinute().list
        }
      }
    }

  }

}
