package com.cadence.model

import org.specs2.mutable.Specification

/**
 * Created by aparrish on 7/30/14.
 */
class RepositorySpec extends Specification {

  import com.cadence.model.repository._
  import com.cadence.model._

  sequential

  "The insert and list" should {

    "drop everything first" in {
      dropAll()
      listUsers().length must be equalTo 0
    }

    "support inserting" in {
      val c = CadenceUser(None, "aparrish@neosavvy.com", "anything")
      val r = addUser(c)

      r must be equalTo 1
    }

    "support retrieving" in {
      val r = listUsers()
      r.length must be equalTo 1
      r(0).email must be equalTo "aparrish@neosavvy.com"
    }
  }

}
