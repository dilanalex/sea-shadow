package controllers

import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import play.api._
import com.mongodb.casbah.commons.MongoDBObject
import models._
import com.mongodb.casbah.Imports._
import com.novus.salat._
import models.salatctx._
import helpers._
import helpers.ProductTaxCalculator
import controllers.backoffice.ShippingOptions
import helpers.PaymentManager
import java.util.Calendar

object OrderCheckOut extends Controller {

  def orderForm(implicit request: RequestHeader) = Form(
    mapping(
      "shipAddresses" -> mapping(
        "firstName" -> optional(text),
        "lastName" -> optional(text),
        "addLine1" -> optional(text),
        "addLine2" -> optional(text),
        "city" -> optional(text),
        "postCode" -> optional(text),
        "country" -> optional(text),
        "state" -> optional(text))(DeliveryAddress.apply) //bind DeliveryAddress
        (DeliveryAddress.unapply),
      "billAddresses" -> mapping(
        "addLine1" -> optional(text),
        "addLine2" -> optional(text),
        "city" -> optional(text),
        "postCode" -> optional(text),
        "country" -> optional(text),
        "state" -> optional(text))(BillingAddress.apply) //bind BillingAddress
        (BillingAddress.unapply) //unbind BillingAddress
        ) //----bind/Unbind Order---
        ((shippingAddr, billingAddr) =>
        Order(shipAddresses = Some(shippingAddr),
          billAddresses = Some(billingAddr)))((order: Order) =>
        Some((order.shipAddresses.getOrElse(new DeliveryAddress()),
          order.billAddresses.getOrElse(new BillingAddress())))))

  def checkout = Action { implicit request =>
    val user = Application.getLoggedInUser(request)
    val order = Cart.getCart(user.email.getOrElse("guest"))
    order.paymentGatewayMethod = None
    Order.save(order)
    Ok(views.html.cart.ordercheckout(
      user, orderForm.fill(order)))
  }

  def submit = Action { implicit request =>
    val user = Application.getLoggedInUser(request)
    var order: Order = Cart.getCart(user.email.getOrElse("guest"))
    val formOrder = orderForm.bindFromRequest().get
    val sAddr = formOrder.shipAddresses.get
    val bAddr = formOrder.billAddresses.get
    var shipAddr: DeliveryAddress =
      DeliveryAddress(
        sAddr.firstName,
        sAddr.lastName,
        sAddr.addLine1,
        sAddr.addLine2,
        sAddr.city,
        sAddr.postCode,
        sAddr.country,
        sAddr.state)
    var billAddr: BillingAddress =
      BillingAddress(
        bAddr.addLine1,
        bAddr.addLine2,
        bAddr.city,
        bAddr.postCode,
        bAddr.country,
        bAddr.state)
    order.shipAddresses = Some(shipAddr)
    order.billAddresses = Some(billAddr)

    order = ProductTaxCalculator.addTax(order)

    Order.save(order)

    models.User.findOne(MongoDBObject("email" ->
      request.session.get("email"))).map { user =>
      Profile.saveDeliveryAddr(user, shipAddr)
      Profile.saveBillingAddr(user, billAddr)
    }

    /*Ok("" + grater[Order].toPrettyJSON(order) + "\n" + 
    grater[Order].toPrettyJSON(formOrder))*/
    //Ok(views.html.order.show(user, order))//Uncomment this

    var shippingOpt = ShippingOptions
      .findShipTRlists(order.shipAddresses.get.country.getOrElse(""),
        order.shipAddresses.get.state.getOrElse(""))
    Ok(views.html.cart.ordershippingoptions(user, order, shippingOpt))
  }

  def saveshippingopt = Action { implicit request =>
    val user = Application.getLoggedInUser(request)
    var order: Order = Cart.getCart(user.email.getOrElse("guest"))
    var optship = request.body.asFormUrlEncoded.get("optship").head.toString()

    //val shipMethod = optship.substring(0, optship.indexOf("#"))
    // val shipFee = optship.substring(optship.indexOf("#") + 1, optship.size)
    val shipOpt = ShippingTableRate.findOneById(new ObjectId(optship)).get

    order.shipMethod = Some(shipOpt.name)
    order.shipMethodId = Some(new ObjectId(optship))
    var shippingOpt = ShippingOptions
      .findShipTRlists(order.shipAddresses.get.country.getOrElse(""),
        order.shipAddresses.get.state.getOrElse(""))
    shippingOpt.foreach(sOpt => {
      if (optship.equals(sOpt._4)) {
        order.shipFee = sOpt._3.toDouble
      }
    })

    order = ShippingTaxCalculator.addTax(order)
    order = ProductTaxCalculator.addTax(order)

    order.chargeCurrency = Some(CurrencyConvertor.getCode(request))
    // Rate shoule be also saved at this point
    //    order.chargeCurrencyRate = 

    order.chargeCurrencyTotal = CurrencyConvertor.getRate(request,
      order.subTotal + order.shipFee + order.shipTax)
    Order.save(order)
    Ok(views.html.cart.orderreview(user, order))

  }

  def paymentValidator = Action { implicit request =>
    //Now call the api and match the order with item,
    if (request.queryString.get("tx").isDefined) {
      val response = PaymentManager.getTransactionDetails(request.queryString.get("tx").get.head)

      if (response.get("success").get == true) {

        val orderId = response.get("INVNUM").get.toString
        val order = models.Order.findOneById(new ObjectId(orderId)).get

        val paymentCurrencyTotal = response.get("paymentCurrencyTotal").get
        val orderCurrencyTotal = order.chargeCurrencyTotal
        val paymentCurrency = response.get("paymentCurrency").get
        val orderCurrency = order.chargeCurrency.get

        if (paymentCurrencyTotal == orderCurrencyTotal
          && paymentCurrency.equals(orderCurrency)) {

          //Change the CART state into ORDER save
          order.paymentGatewayMethod = Some(PGMethod.PPPaymentPro)
          order.state = OrderState.order
          order.date = Some(Calendar.getInstance().getTime())
          Order.save(order)

          Redirect(routes.OrderCheckOut.confirmationNew(order.id))

        } else {

          Ok(views.html.cart.paymentfail(null))
        }

      } else {

        Ok(views.html.cart.paymentfail(null))
      }
    } else {

      Ok(views.html.cart.paymentfail(null))
    }

  }

  def confirmationNew(id: ObjectId) = Action { implicit request =>
    val user = Application.getLoggedInUser(request)
    val order = models.Order.findOneById(id).get
    Ok(views.html.order.show(user, order))
  }

  def confirmation = Action { implicit request =>
    val user = Application.getLoggedInUser(request)
    var orderId = request.body.asFormUrlEncoded.get("order").head.toString()
    val order = models.Order.findOneById(new ObjectId(orderId)).get
    order.state = OrderState.order //Change the CART state into ORDER
    order.date = Some(Calendar.getInstance().getTime())
    Order.save(order)
    Ok(views.html.order.show(user, order))
  }

  def parseObjectId(body: AnyContent, field: String): ObjectId = {
    new ObjectId(body.asFormUrlEncoded.get.get(field).get.head)
  }

}
