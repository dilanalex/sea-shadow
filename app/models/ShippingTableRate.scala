package models

import play.api.Play.current
import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._

case class ShippingTableRate(
  id: ObjectId = new ObjectId,
  var name: String = "",
  var description: Option[String] = None,
  var taxableFlag: Option[Boolean] = None,
  var enableFrontFlag: Option[Boolean] = None,
  var enableAdminFlag: Option[Boolean] = None,
  var handlingFee: Option[String] = None,
  var minWeight: Option[String] = None,
  var maxWeight: Option[String] = None,
  var obcAPI: Option[String] = None,
  var rate: List[TableRate] = List.empty,
  var visibility: Option[List[String]] = None,
  var countries: Option[List[String]] = None //var rate: Option[List[TableRate]] = None //temporary added
  ) {
  def addCustomerGroups(list: List[String]) {
    visibility = Some(list)
  }

  def addCountries(list: List[String]) {
    countries = Some(list)
  }

}

case class TableRate(
  var country: String = "", // reference to country id
  var state: String = "", // reference to state id
  var zipCode: Option[String] = None,
  var city: Option[String] = None,
  var minWeight: Option[String] = None,
  var maxWeight: Option[String] = None,
  var minVolumn: Option[String] = None,
  var maxVolumn: Option[String] = None,
  var minSubTotal: Option[String] = None,
  var maxSubTotal: Option[String] = None,
  var minItems: Option[String] = None,
  var maxItems: Option[String] = None,
  var rate: String = "0.00")

object ShippingTableRate extends ModelCompanion[ShippingTableRate, ObjectId] {
  val collection = mongoCollection("shipping_tablerate")
  val dao = new SalatDAO[ShippingTableRate, ObjectId](collection = collection) {}
}