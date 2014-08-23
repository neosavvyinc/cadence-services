package com.cadence.model

import org.specs2.mutable.Specification

/**
 * Created by aparrish on 7/30/14.
 */
class RepositorySpec extends Specification {

  import com.cadence.model.repository._
  import com.cadence.model._

  sequential

  "The insert and list of users" should {

    "drop everything first" in {
      dropAll()
      listUsers().length must be equalTo 0
    }

    "support inserting" in {
      val c = CadenceUser(None, "aparrish@neosavvy.com", "adam", "parrish", "neosavvy")
      val r = addUser(c)

      r must be greaterThan(0)
    }

    "support retrieving" in {
      val r = listUsers()
      r.length must be equalTo 1
      r(0).email must be equalTo "aparrish@neosavvy.com"
    }

    "support updating the user" in {
      val r = listUsers()
      updateUser(r(0).copy( firstName = "Trevor", lastName = "Ewen"))

      val n = listUsers()
      (n(0).firstName must be equalTo "Trevor") and (n(0).lastName must be equalTo "Ewen")
    }

    "support finding one user by id" in {
      dropAll();
      val a = CadenceUser(None, "aparrish@neosavvy.com", "adam", "parrish", "neosavvy")
      val t = CadenceUser(None, "trevor@neosavvy.com", "trevor", "ewen", "neosavvy")
      val c = CadenceUser(None, "chris@neosavvy.com", "chris", "caplinger", "neosavvy")

      val aId = addUser(a)
      println("Adams's ID: " + aId)
      val tId = addUser(t)
      println("Trevor's ID: " + tId)
      val cId = addUser(c)
      println("Chris's ID: " + cId)

      val adam = findById(aId)
      val trevor = findById(tId)
      val chris = findById(cId)

      (adam.get.firstName must be equalTo "adam")
      (adam.get.lastName must be equalTo "parrish")
    }
  }

}
