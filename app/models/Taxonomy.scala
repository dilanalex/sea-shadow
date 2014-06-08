package models

import play.api.Play.current

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import scala.collection.mutable.MutableList

import models.salatctx._

case class Taxonomy(
  id: ObjectId,
  name: String,
  path: String,
  parent: Option[ObjectId] = None,
  var ancestors: List[ObjectId] = List())

object Taxonomy extends ModelCompanion[Taxonomy, ObjectId] {
  val collection = mongoCollection("Taxonomy")
  val dao = new SalatDAO[Taxonomy, ObjectId](collection = collection) {}

  collection.ensureIndex(MongoDBObject("path" -> 1), "path_index", true)
  collection.ensureIndex(MongoDBObject("ancestors" -> 1), "ancestors_index",
    false)
}