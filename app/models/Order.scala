package models

import scala.collection.mutable.MutableList
import play.api.Play.current
import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._
import scala.math.BigDecimal
import java.util.Date

object OrderState extends Enumeration {
  type OrderState = Value
  val cart, order, process, shipped, closed = Value
}

object PGMethod extends Enumeration {
  type PGMethod = Value
  val PPExpressCheckout, PPPaymentPro = Value
}

case class LineItem(
  product: ObjectId, // reference to product id
  sku: String,
  title: String,
  var qty: Int = 1,
  var tax: BigDecimal = 0,
  price: BigDecimal,
  image: String)

case class Order(
  id: ObjectId = new ObjectId,
  var userEmail: Option[String] = None,
  var state: OrderState.Value = OrderState.cart,
  // Default starting state always cart
  var lineItems: List[LineItem] = Nil,
  var subTotal: BigDecimal = 0,
  var productTax: BigDecimal = 0,
  var shipMethodId: Option[ObjectId] = None,
  var shipMethod: Option[String] = None,
  var shipFee: BigDecimal = 0,
  var shipTax: BigDecimal = 0,
  var chargeCurrencyTotal: BigDecimal = 0,
  var chargeCurrency: Option[String] = None,
  var chargeCurrencyRate: BigDecimal = 0,
  var shipAddresses: Option[DeliveryAddress] = None,
  var billAddresses: Option[BillingAddress] = None,
  var paymentGatewayMethod: Option[PGMethod.Value] = None,
  var date: Option[Date] = None) {

  def addProduct(product: Product) {
    val index =
      lineItems.indexWhere((item: LineItem) => item.product == product.id)
    if (index >= 0) {
      lineItems(index).qty += 1
    } else {
      lineItems ::=
        LineItem(
          product = product.id,
          sku = product.sku,
          title = product.title,
          price = product.price,
          image = product.image.head)
    }
    subTotal += product.price
  }

  def removeProduct(product: Product) {
    val index =
      lineItems.indexWhere((item: LineItem) => item.product == product.id)
    if (index >= 0) {
      if (lineItems(index).qty == 1) {
        lineItems =
          lineItems.filterNot((item: LineItem) => item.product == product.id)
      } else {
        lineItems(index).qty -= 1
      }
      subTotal -= product.price
    }
  }

  def deleteProduct(product: Product) {
    val index =
      lineItems.indexWhere((item: LineItem) => item.product == product.id)
    if (index >= 0) {
      subTotal -= (product.price * lineItems(index).qty)
      lineItems =
        lineItems.filterNot((item: LineItem) => item.product == product.id)

    }
  }

}

object Order extends ModelCompanion[Order, ObjectId] {
  val collection = mongoCollection("orders")
  val dao = new SalatDAO[Order, ObjectId](collection = collection) {}
}
