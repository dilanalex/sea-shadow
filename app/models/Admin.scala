package models

import scala.collection.mutable.MutableList
import play.api.Play.current

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._

case class Admin(
  id: ObjectId = new ObjectId,
  var username: String = "",
  var name: Option[String] = None,
  var password: String = "",
  var adminType : String = "") {

}

object Admin extends ModelCompanion[Admin, ObjectId] {

  val collection = mongoCollection("admin")

  val dao = new SalatDAO[Admin, ObjectId](collection = collection) {}

}