package controllers

import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import play.api._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import models.Order
import models.salatctx._
import models._
import securesocial.core.SocialUser
import securesocial.core.SecureSocial


/** This provides controllers to show the order history
 *
 */
object Orders extends Controller with SecureSocial {
 
  def list = SecuredAction() {
    implicit request =>
      User.findOne(MongoDBObject("socialUser.email" -> request.user.email.get)).map { user =>
        val orders = Order.find(
          MongoDBObject("userEmail" -> user.socialUser.email.get, "state" ->
            OrderState.order.toString())).toList
        Ok(views.html.order.list(request.user, orders))

      }.getOrElse(Forbidden)
  }

  def show(id: ObjectId) = Action { implicit request =>
    val order = models.Order.findOneById(id).get
    val user = Application.getLoggedInUser(request)
    Ok(views.html.order.show(user, order))
  }

}
