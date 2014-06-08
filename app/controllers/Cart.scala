package controllers

import play.api._
import play.api.mvc._
import com.mongodb.casbah.Imports._
import com.novus.salat._
import models._
import models.salatctx._
import models.Order

object Cart extends Controller {

  def show = Action { implicit request =>
    val user = Application.getLoggedInUser(request)
    Ok(views.html.cart.show(getCart(user.email.getOrElse("guest")), user))
  }

  def add() = Action { implicit request =>
    // Retrieve the full product - should use get or else and return error on not found
    val product =
      models.Product.findOneById(parseObjectId(request.body, "productid")).get
    val userEmail = Application.getLoggedInUser(request).email.getOrElse("guest")
    val order = getCart(userEmail)

    order.addProduct(product)
    Order.save(order)

    Redirect(routes.Cart.show)
  }

  def remove() = Action { implicit request =>
    // Retrieve the full product - should use get or else and return error on not found
    val product =
      models.Product.findOneById(parseObjectId(request.body, "productid")).get
    val userEmail = Application.getLoggedInUser(request).email.getOrElse("guest")
    val order = getCart(userEmail)

    order.removeProduct(product)
    Order.save(order)

    Redirect(routes.Cart.show)
  }

  // Deletes a product form cart
  def delete() = Action { implicit request =>
    // Retrieve the full product - should use get or else and return error on not found
    val product =
      models.Product.findOneById(parseObjectId(request.body, "productid")).get
    val userEmail = Application.getLoggedInUser(request).email.getOrElse("guest")
    val order = getCart(userEmail)

    order.deleteProduct(product)
    Order.save(order)

    Redirect(routes.Cart.show)
  }

  def parseObjectId(body: AnyContent, field: String): ObjectId = {
    new ObjectId(body.asFormUrlEncoded.get.get(field).get.head)
  }

  def getCart(usrEmail: String): models.Order = {    
    Order.findOne(MongoDBObject("userEmail" -> usrEmail, 
    							"state" -> OrderState.cart.toString())).getOrElse({
    var order = models.Order()
    order.userEmail= Some(usrEmail);
    order
    })    
  }
    
}
