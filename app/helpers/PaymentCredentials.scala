package helpers

object PaymentCredentials {

  //Sandbox

  val apiUrl = "https://api-3t.sandbox.paypal.com/nvp"
  val user = "orange_1348479377_biz_api1.yahoo.com"
  val pwd = "1348479419"
  val version = "72.0"
  val signature = "AiPC9BjkCyDFQXbSkoZcgqH3hpacADiyJwMUle2hAokTvqM0DQrRksCS"

  //#################  Paypal payments pro ###############
  val ppProPaymentUrl = "https://securepayments.sandbox.paypal.com/acquiringweb"
  val ppProBusiness = "L5FRFEMNVZMU6"

  //#################  Paypal Express Checkout ###############
  val ppExChkRedirectUrl = "https://www.sandbox.paypal.com/webscr?cmd=_express-checkout&token="  

  object paymentAction extends Enumeration {
    type paymentAction = Value
    val Sale = Value("Sale").toString()
    val Authorization = Value("Authorization").toString()
    val Order = Value("Order").toString()
  }

  object paymentMethod extends Enumeration {
    type paymentMethod = Value
    val SetExpressCheckout = Value("SetExpressCheckout").toString()
    val GetExpressCheckoutDetails = Value("GetExpressCheckoutDetails").toString()
    val DoExpressCheckoutPayment = Value("DoExpressCheckoutPayment").toString()
  }

  //------------End Sandbox Express Checkout Credentials---------------  

  //Paypal Sandbox account login information
  //  login name : orangebus_uk@yahoo.com
  //  login pwd : orangebus123

  //  sandbox merch account : orange_1348479377_biz@yahoo.com
  //  pwd  orangebus123

  //  for other personal accounts use pwd tester123 

}