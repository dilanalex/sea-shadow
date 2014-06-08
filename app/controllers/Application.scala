package controllers

import play.api._
import play.api.mvc._
import play.api.i18n.Messages
import play.api.i18n.Lang
import securesocial.core.SecureSocial
import models._
import securesocial.core.SocialUser
import securesocial.core.SocialUser
import securesocial.core.SocialUser
import org.bson.types.ObjectId
import securesocial.core.UserId
import securesocial.core.AuthenticationMethod


object Application extends Controller with SecureSocial {  

  def login = SecuredAction() { implicit request =>
    //println(request.user)    
    Ok(views.html.index(Messages("home.big.label"), request.user))
  }
  
  def index = Action { implicit request =>     
    Ok(views.html.index(Messages("home.big.label"),
        SecureSocial.currentUser[AnyContent](request).getOrElse(null)))
  }
  
  def getLoggedInUser(request: play.api.mvc.Request[play.api.mvc.AnyContent]) : SocialUser = {    
    val user = SecureSocial.currentUser[AnyContent](request).getOrElse({
      val objId = new ObjectId().toString()
      val tempGuestUser = new SocialUser(UserId(objId,"userpass"), 
          "guest", Some("guest@guest.com"), Some(""), AuthenticationMethod.UserPassword) 
          // TODO: Think how to handle users who are not logged in     
      tempGuestUser
    })
    
    /*Session session = play.mvc.Http.Context .current().session()
    Context.current().request().getHeader("user")
    Context.current().session().put("user", user.id.id)
    var userName = session.get("userName")*/
    user
  }
  
  // -- Javascript routing
  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    import backoffice.routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
      States.getCountryStateList, 
      ShippingOptions.findShipTRlist,
      Currencies.setCurrency,
      Product.listproducts,
      Product.listRelatedProducts,
      PriceRules.addCondition,
      PriceRules.selectedProducts,
      PriceRules.listproducts,
      PriceRules.displaySelectedProducts,
      PriceRules.displaySelectedProduct,
      PriceRules.updatePriorityOrder,
       PriceRules.addConditionSet,
       OrderProcess.changeStatus
      )
    ).as("text/javascript") 
  }
  
  
}