package com.cadence.model

import org.specs2.mutable.Specification

/**
 * Created by aparrish on 7/30/14.
 */
class RepositorySpec extends Specification {

  import com.cadence.model.repository._
  import com.cadence.model._

  sequential

  def basicUsers() = {
    val a = CadenceUser(None, "adam", "parrish", "aparrish@neosavvy.com", "neosavvy", "A")
    val t = CadenceUser(None, "trevor", "ewen", "trevor@neosavvy.com", "neosavvy", "B")
    val c = CadenceUser(None, "chris", "caplinger", "chris@neosavvy.com","neosavvy", "C")

    val aId = addUser(a)
    val tId = addUser(t)
    val cId = addUser(c)
    (aId,tId,cId)
  }

  "The insert and list of users" should {

    "drop everything first" in {
      dropAll()
      listUsers().length must be equalTo 0
    }

    "support inserting" in {
      val c = CadenceUser(None, "adam", "parrish", "aparrish@neosavvy.com", "neosavvy", "a")
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
      val (aId,tId,cId) = basicUsers();

      val adam = findById(aId)
      val trevor = findById(tId)
      val chris = findById(cId)

      (adam.get.firstName must be equalTo "adam")
      (adam.get.lastName must be equalTo "parrish")
    }

    "support finding one user by email " in {
      dropAll();
      val (aId,tId,cId) = basicUsers();

      val r = findByEmail("aparrish@neosavvy.com");

      r.get.id must be equalTo Some(aId)
    }
  }

}
