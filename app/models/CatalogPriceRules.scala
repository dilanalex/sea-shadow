package models

import play.api.Play.current
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._
import java.util.{Date}

case class RuleCondition(
  id: ObjectId = new ObjectId,
  attribute: String,
  var operator: Option[String],  
  var cvalue: Option[String],
  var products: List[Tuple2[String, String]] = Nil
 )
  
case class CatalogPriceRules(
  id: ObjectId = new ObjectId,
  var name: String = "",
  var description: String = "",
  var active: Option[Boolean] = None,
  var dateStart: Option[Date] = None,
  var dateEnd: Option[Date] = None,
  var customerGroups: Option[List[String]] = None,
  var conditions: List[RuleCondition] = Nil,
  var terminatingRule: Option[Boolean] = None,
  var action: String = "",
  var discountAmount: BigDecimal = 0.00,
  var priority : Int = 1
  )
  {
    def addCustomerGroups(list: List[String]) {
      customerGroups = Some(list)
    }
  }

object CatalogPriceRules extends ModelCompanion[CatalogPriceRules, ObjectId] {
  val collection = mongoCollection("catelog_price_rules")
  val dao = new SalatDAO[CatalogPriceRules, ObjectId](collection = collection) {}
}