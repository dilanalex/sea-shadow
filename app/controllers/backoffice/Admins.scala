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

object Admins extends Controller with Secured{
	 
	def adminForm(implicit request: RequestHeader): Form[Admin] = Form(
    mapping(
      "adminId" -> nonEmptyText,
      "username" -> nonEmptyText,
      "name" -> optional(text),
      "password" -> nonEmptyText,
      "adminType" -> text)((
        adminId,
        username,
        name,
        password,
        adminType) =>
        Admin(
          id = new ObjectId(adminId),
          username = username,
          name = name,
          password = password,
          adminType = adminType))(
        admin =>
          Some(admin.id.toString,
            admin.username,
            admin.name,
            admin.password,
            admin.adminType)))
	 
	 
	 
	//list down all backoffice links
	/*def list() = Action { implicit request =>
    	Ok(views.html.backoffice.admin.list())
  	}*/
  	
  	def listusers() = Action { implicit request =>
  		val users = Admin.findAll().toList.sortBy(c => c.name)
    	Ok(views.html.backoffice.admin.listusers(users))
  	}
  	
  	def list = IsAuthenticated { username =>
    implicit request =>
      Admin.findOne(MongoDBObject("username" -> username)).map 
      { admin =>
        var usrToSend: Admin =
          new Admin(new ObjectId, admin.username, Option(""), admin.password, admin.adminType)
        Ok(views.html.backoffice.admin.list(usrToSend))
      }.getOrElse(Forbidden)
  	}
  	
  	def show(id: ObjectId) = Action { implicit request =>
    	Ok(views.html.backoffice.admin.signup(adminForm.fill(getAdmin(id))))
    }
    
    private def getAdmin(id: ObjectId): Admin = {
    	Admin.findOne(MongoDBObject("_id" -> id)).getOrElse(new Admin)
  	}
  	
  	def save = Action { implicit request =>

	    var adminId =
	      request.body.asFormUrlEncoded.get("adminId").head.toString()
	    
	    adminForm.bindFromRequest.fold(form => {
	      BadRequest(views.html.backoffice.admin.signup(form))
	    },
	      adminForm => {
	
	        if (Admin.findOne(
	          MongoDBObject("_id" -> new ObjectId(adminId))).isDefined) {
	
			 Admin.update(MongoDBObject("_id" ->
	            new ObjectId(adminId)), grater[Admin].asDBObject(adminForm), false, false)
	          	
	          Redirect(controllers.backoffice.routes.Admins.show(
	            new ObjectId(adminId)))
	            .flashing("message" -> "Admin updated !!!")
	        } else {
	          adminForm.adminType = "NORMAL"
	          Admin.save(adminForm)
	          Redirect(controllers.backoffice.routes.Admins.show(
	            new ObjectId())).flashing("message" -> "Admin Saved !!!")
	        }
	      })
	  }
  	
  	def delete(id: ObjectId) = Action { implicit request =>
	    val admin = getAdmin(id)
	
	    if (admin.adminType == "SUPER") {
	
	      Redirect(controllers.backoffice.routes.Admins.show(id))
	        .flashing("errormessage" ->
	          Messages( "backoffice.admin.super.admin.delete.err" ))
	    } else {
	
	      Admin.remove(MongoDBObject("_id" -> id))
	
	      Redirect(controllers.backoffice.routes.Admins.listusers())
	        .flashing("message" -> Messages( "backoffice.admin.deleted" ))
	    }
  }
  	
}