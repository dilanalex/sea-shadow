package models

import scala.collection.mutable.MutableList
import play.api.Play.current

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._

case class State(
  id: ObjectId = new ObjectId,
  var countryId: ObjectId = new ObjectId,
  var code: Option[String] = None,
  var name: Option[String] = None) {

}

object State extends ModelCompanion[State, ObjectId] {

  val collection = mongoCollection("states")

  val dao = new SalatDAO[State, ObjectId](collection = collection) {}

}