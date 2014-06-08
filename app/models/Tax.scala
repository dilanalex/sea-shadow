package models

import play.api.Play.current

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._

case class Tax(
  id: ObjectId = new ObjectId,
  var name: String = "",
  var description: String = "",
  var defaultFlag: Option[Boolean] = None,
  var rate: List[Rate] = List.empty
)
	
case class Rate(
  var country: String = "", // reference to country id
  var state: String = "", // reference to state id
  var zipCode: Option[String] = None,
  var city: Option[String] = None,
  var rate: BigDecimal = 0.00,
  var priority: Int = 0,
  var taxName: String = "",
  var compound: Option[Boolean] = None
)

object Tax extends ModelCompanion[Tax, ObjectId] {
  val collection = mongoCollection("tax")
  val dao = new SalatDAO[Tax, ObjectId](collection = collection) {}
  
  val list = (
  	this.findAll.toList
  )
}