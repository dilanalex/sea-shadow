package models

import scala.collection.mutable.MutableList
import play.api.Play.current

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._

case class Currency(
  id: ObjectId = new ObjectId,
  var iso: String = "",
  var name: String = "",
  var symbol: String = "",
  var rate: BigDecimal = 0.00,
  var enabled: Option[Boolean] = None,
  var baseCurrency: Option[Boolean] = None)

object Currency extends ModelCompanion[Currency, ObjectId] {
  val collection = mongoCollection("currencies")
  val dao = new SalatDAO[Currency, ObjectId](collection = collection) {}

  val activeCurrencies = Currency.find(MongoDBObject("enabled" -> ("true".toBoolean))).toList.sortBy(c => c.name)
  val baseCurrency = Currency.findOne(MongoDBObject("enabled" -> ("true".toBoolean), "baseCurrency" -> ("true".toBoolean))).getOrElse(null)  
}