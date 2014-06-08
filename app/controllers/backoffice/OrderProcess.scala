package controllers.backoffice

import play.api._
import play.api.mvc._
import com.mongodb.casbah.Imports._
import com.novus.salat._
import models.salatctx._
import models._
import helpers.Page
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.Forms._
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.i18n.Messages


object OrderProcess extends Controller with Secured{

	//list down existing orders
    def listOrders() = Action { implicit request =>
  		val orders = Order.findAll().toList.sortBy(c => c.date)
    	Ok(views.html.backoffice.orderprocess.listorders(orders))
  	}
  	
  	//shows details of specific order
  	def show(id: ObjectId) = Action { implicit request =>
      val order = models.Order.findOneById(id).get
      //val user = Application.getLoggedInUser(request)
      Ok(views.html.backoffice.orderprocess.show(order))
    }
    
    def changeStatus(status : String, orderId : String) = Action { implicit request =>
  		//val orders = Order.findAll().toList.sortBy(c => c.date)
  		//id : new MongoId(orderId)
  		
  		
          //Order.update(MongoDBObject("_id" -> orderId), $set("state"->status), false, false)
          if (Order.findOne(
          MongoDBObject("_id" -> new ObjectId(orderId))).isDefined){
          System.out.println("object found");
          //System.out.println(Order.update(MongoDBObject("_id" -> orderId), MongoDBObject("state" -> "bridge player"), 
      //upsert = true, multi = true, wc = new WriteConcern))
      Order.update(MongoDBObject("_id" -> new ObjectId(orderId)), $set("state" -> status), false, false, WriteConcern.Safe)
          }else{System.out.println("Order cannot find")}
  		val ret = """xxx"""
    	Ok(ret)
  	}

}
	