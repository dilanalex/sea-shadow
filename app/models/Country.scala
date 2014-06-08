package models

import scala.collection.mutable.MutableList
import play.api.Play.current

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._

case class Country(
  id: ObjectId = new ObjectId,
  var code: String = "",
  var name: String = "",
  var code3: String = "",
  var codeIsoNumeric: Int = 0,
  var enabled: Option[Boolean] = None,
  var enabledInAA: Option[Boolean] = None
)

object Country extends ModelCompanion[Country, ObjectId] {
  val collection = mongoCollection("countries")
  val dao = new SalatDAO[Country, ObjectId](collection = collection) {}
  
  val countriesAct = Country.find(MongoDBObject("enabled" -> ("true".toBoolean))).toList.sortBy(c => c.name)
}