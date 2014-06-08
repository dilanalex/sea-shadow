package models

import play.api.Play.current

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._

import models.salatctx._

case class Product(
  id: ObjectId = new ObjectId,
  var sku: String = "",
  var title: String = "",
  var price: BigDecimal = 0.00,
  var description: String = "",
  var image: List[String] = List("no_Image.jpg"),
  var taxClassId: Option[String] = None,
  var categories: Set[ObjectId] = Set.empty,
  var relatedProducts: List[String] = List(),
  var metaDescription: Option[String] = None,
  var metaKeywords: Option[String] = None
)

object Product extends ModelCompanion[Product, ObjectId] {
  val collection = mongoCollection("products")
  val dao = new SalatDAO[Product, ObjectId](collection = collection) {}
}
