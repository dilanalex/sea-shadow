package controllers

import play.api._
import play.api.mvc._
import models._
import models.salatctx._
import com.mongodb.casbah.Imports._
import controllers.utils._
import java.net.URLDecoder
import securesocial.core.SecureSocial
import helpers.CurrencyConvertor
import play.api.i18n.Messages
import helpers._
import controllers.backoffice.ShippingOptions
import java.io.InputStream

object Payments extends Controller {  

  def paypalSetExpressCheckout = Action { implicit request =>    
    val userEmail = Application.getLoggedInUser(request).email.getOrElse("guest")
    val order = Cart.getCart(userEmail)
    order.paymentGatewayMethod = Some(PGMethod.PPExpressCheckout)
    Order.save(order)
    var data = getPayPalMerchantInfo()
 
    data += ("PAYMENTREQUEST_0_PAYMENTACTION" -> PaymentCredentials.paymentAction.Order) 
    var i = 0
    var subtotal : BigDecimal = 0.00
    var itemAmt : BigDecimal = 0.00
    order.lineItems.foreach( ln => {
      data += ("L_PAYMENTREQUEST_0_NAME"+i -> ln.title)
      //data += ("L_PAYMENTREQUEST_0_DESC"+i -> "xxxxDesc")
      itemAmt = CurrencyConvertor.getRate(request, ln.price)
      subtotal = subtotal + itemAmt
      data += ("L_PAYMENTREQUEST_0_AMT"+i -> itemAmt.toString)
      //println("ITEM PRICE="+CurrencyConvertor.getRate(request, ln.price).toString)
      data += ("L_PAYMENTREQUEST_0_QTY"+i -> ln.qty.toString)
      i = i + 1
    })
    //println("SUB TOTAL="+CurrencyConvertor.getRate(request, order.subTotal).toString)
    data += ("PAYMENTREQUEST_0_AMT" -> subtotal.toString)
    //data += ("PAYMENTREQUEST_0_DESC" -> "xxxx111111111")    
    //data += ("PAYMENTREQUEST_0_ITEMAMT" -> "20.00")
    
    data += ("PAYMENTREQUEST_0_CURRENCYCODE" -> CurrencyConvertor.getCode(request))
  
    data += ("RETURNURL" -> controllers.routes.Payments.paypalGetExpressCheckoutDetails.absoluteURL(false))
    data += ("CANCELURL" -> getHost(request))
    data += ("METHOD" -> PaymentCredentials.paymentMethod.SetExpressCheckout)
    //println("data="+data)
    var retData = Common.Post(PaymentCredentials.apiUrl, data)
    val res = readResponseData(retData)

    if(res.contains("ACK") && res.get("ACK").get.equals("Success")){
      val tokenValue = res.get("TOKEN").get  
      Redirect(PaymentCredentials.ppExChkRedirectUrl+tokenValue)
    } else {
      Ok(views.html.index(Messages("home.big.label"), SecureSocial.currentUser[AnyContent](request).getOrElse(null)))
    }
  
  }

  def paypalGetExpressCheckoutDetails = Action { implicit request =>
    val token = request.queryString.get("token").get.apply(0)
    val payerID = request.queryString.get("PayerID").get.apply(0)
    //println("PayerID="+request.get("PayerID"))
    var data = getPayPalMerchantInfo()
    data += ("TOKEN" -> token)
    data += ("METHOD" -> PaymentCredentials.paymentMethod.GetExpressCheckoutDetails)
    var retData = Common.Post(PaymentCredentials.apiUrl, data)
    val res = readResponseData(retData)  
  
    val user = Application.getLoggedInUser(request)
    val userEmail = user.email.getOrElse("guest")
    var order = Cart.getCart(userEmail)
  
    var shipAddr: DeliveryAddress =
      DeliveryAddress(
        res.get("FIRSTNAME"),
        res.get("LASTNAME"),
        res.get("SHIPTOSTREET"),
        Some(""),//addLine2
        res.get("SHIPTOCITY"),
        res.get("SHIPTOZIP"),
        res.get("SHIPTOCOUNTRYCODE"),
        res.get("SHIPTOSTATE"))
    var billAddr: BillingAddress =
      BillingAddress(
        res.get("SHIPTOSTREET"),
        Some(""),//addLine2
        res.get("SHIPTOCITY"),
        res.get("SHIPTOZIP"),
        res.get("SHIPTOCOUNTRYCODE"),
        res.get("SHIPTOSTATE"))
    order.shipAddresses = Some(shipAddr)
    order.billAddresses = Some(billAddr)

    order = ProductTaxCalculator.addTax(order)
    Order.save(order)

    models.User.findOne(MongoDBObject("email" ->
      request.session.get("email"))).map { user =>
        Profile.saveDeliveryAddr(user, shipAddr)
        Profile.saveBillingAddr(user, billAddr)
      }

    var shippingOpt = ShippingOptions.findShipTRlists(order.shipAddresses.get.country.getOrElse(""),
      order.shipAddresses.get.state.getOrElse(""))
    Ok(views.html.cart.ordershippingoptions(user, order, shippingOpt)).withSession(session+
        ("token" -> token)+
        ("PayerID" -> payerID)
        )
  }

  def paypalDoExpressCheckoutPayment = Action { implicit request =>
    val user = Application.getLoggedInUser(request)
    val userEmail = user.email.getOrElse("guest")
    var order = Cart.getCart(userEmail)
    val token = request.session.get("token").getOrElse("").toString()
    val payerID = request.session.get("PayerID").getOrElse("").toString()
    
    var subTotal = CurrencyConvertor.getRate(request, (order.subTotal))
    var productTax = CurrencyConvertor.getRate(request, (order.productTax))    
    var totalshpChg = CurrencyConvertor.getRate(request, (order.shipFee + order.shipTax))
   
    var data = getPayPalMerchantInfo()
    data += ("PAYMENTREQUEST_0_PAYMENTACTION" -> PaymentCredentials.paymentAction.Order)
    data += ("PAYERID" -> payerID)
    data += ("TOKEN" -> token)
    
    data += ("PAYMENTREQUEST_0_CURRENCYCODE" -> CurrencyConvertor.getCode(request))
    data += ("PAYMENTREQUEST_0_AMT" -> (subTotal + totalshpChg).toString)
    data += ("PAYMENTREQUEST_0_TAXAMT" -> productTax.toString)
    data += ("PAYMENTREQUEST_0_SHIPPINGAMT" -> totalshpChg.toString)
    data += ("PAYMENTREQUEST_0_ITEMAMT" -> (subTotal - productTax).toString)    
    data += ("METHOD" -> PaymentCredentials.paymentMethod.DoExpressCheckoutPayment)
    //println("data="+data)
    var retData = Common.Post(PaymentCredentials.apiUrl, data)
    val res = readResponseData(retData)
    
    if(res.contains("ACK") && res.get("ACK").get.equals("Success")){
      order.state = OrderState.order //Change the CART state into ORDER
      Order.save(order)
      Ok(views.html.order.show(user, order))
    } else {
      Ok(views.html.cart.paymentfail(SecureSocial.currentUser[AnyContent](request).getOrElse(null)))
    }
  
  }
 
  def getPayPalMerchantInfo(): Map[String, String] = {
    var data: Map[String, String] = Map()
    data += ("USER" -> PaymentCredentials.user)  
    data += ("PWD" -> PaymentCredentials.pwd)
    data += ("SIGNATURE" -> PaymentCredentials.signature)
    data += ("VERSION" -> PaymentCredentials.version)    
    data
  }
  
  def readResponseData(retData: InputStream): Map[String, String] = {
    var strData = Common.readInputStream(retData)
    strData = URLDecoder.decode(strData,"UTF-8")
    
    //println("XXXX="+strData)
    val res = strData.split('&') map { str =>
      val pair = str.split('=')
      (pair(0) -> pair(1))
    } toMap

    //println(res)
    res
  }  
  
  def getHost(request: play.api.mvc.Request[play.api.mvc.AnyContent]): String = {
    "http://"+request.headers.get("host").getOrElse("")
  }

}