package controllers.backoffice

import play.api.mvc._
import com.novus.salat._
import com.mongodb.casbah.Imports._
import models.salatctx._
import play.api.data._
import models._
import helpers.Page
import play.api.data.Forms._

object TaxRates extends Controller{
  
  def taxRateForm(implicit request: RequestHeader): Form[models.Tax] = Form(
    mapping(
      "taxId" -> nonEmptyText,
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "defaultFlag" -> optional(boolean),
      "rate" -> play.api.data.Forms.list(mapping(
          			"country" -> nonEmptyText.verifying(
          			    "Please enter valid Country", 
          			    country => (!Country.findOne(MongoDBObject("code" -> country)).isEmpty || country.equals("*"))
          			    ),
          			"state" -> nonEmptyText.verifying(
          			    "Please enter valid State", 
          			    state => (!State.findOne(MongoDBObject("code" -> state)).isEmpty || state.equals("*"))
          			    ),
          			"zipCode" -> optional(text),
          			"city" -> optional(text),
          			"rate" -> nonEmptyText.verifying("Please enter valid rate", rate => rate.matches("""^[0-9]\d*(\.\d+)?$""")),
          			"priority" -> nonEmptyText.verifying("Please enter valid priority", priority => priority.matches("""^[0-9]\d*(\.\d+)?$""")),
          			"taxName" -> nonEmptyText,
          			"compound" -> optional(boolean))
          			((
          					country,
          					state,
          					zipCode,
          					city,
          					rate,
          					priority,
          					taxName,
          					compound
          			) => Rate(
          							country = country,
          							state = state,
          							zipCode = zipCode,
          							city = city,
          							rate = rate.toDouble,
          							priority = priority.toInt,
          							taxName = taxName,
          							compound = (compound)
          			))
          			(
          					rate => Some(
          							rate.country,
          							rate.state,
          							rate.zipCode,
          							rate.city,
          							rate.rate.toString(),
          							rate.priority.toString,
          							rate.taxName,
          							rate.compound
          							)
          			))
    )((
    		taxId,
        name,
        description,
        defaultFlag,
        rate
        	) => models.Tax(
        			id = new ObjectId(taxId),
        			name = name,
        			description = description,
        			defaultFlag = (defaultFlag),
        			rate = rate.toList
          ))(
      		tax => Some(
      				tax.id.toString,
      				tax.name,
      				tax.description,
      				tax.defaultFlag,
      				tax.rate)))
      				
      				
      				
          
  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(controllers.backoffice.routes.TaxRates.list(1, 1, ""))
  
  /**
   * Handle default path requests, redirect to computers list
   */  
  def index = Action { Home }
  
  def show(id: ObjectId) = Action { implicit request => 
    Ok(views.html.backoffice.tax.show(taxRateForm.fill(getTax(id)), getTax(id).rate))
  }
  
  def list(page: Int, orderBy: Int, filter: String = "%") = Action {
    implicit request =>
      val offest = 10 * page
      var pattern = ".*(?i)" + filter + ".*";
      var taxRates = models.Tax.find(MongoDBObject("name" -> pattern.r))
        .skip(offest).limit(10).toList
      var records =
        models.Tax.find(MongoDBObject("name" -> pattern.r)).size
      Ok(views.html.backoffice.tax.list(Page(taxRates, page, offest,
        records), orderBy, filter))
  }
  
  def save = Action { implicit request =>
    var taxId =
      request.body.asFormUrlEncoded.get("taxId").head.toString()
    var rates: List[Rate] = List.empty
    if(models.Tax.findOne(MongoDBObject("_id" -> new ObjectId(taxId))).isDefined){ 
      rates = getTax(new ObjectId(taxId)).rate
    }
    taxRateForm.bindFromRequest.fold(form => {
      //println("BAD REQUEST="+taxRateForm)
      BadRequest(views.html.backoffice.tax.show(form, rates)).flashing("errormessage" -> "Incomplete Form!!! Please check...")      
    },
      taxRateForm => {
        if (models.Tax.findOne(
          MongoDBObject("_id" -> new ObjectId(taxId))).isDefined) {
          //println("UPDATE="+taxRateForm)
          models.Tax.update(MongoDBObject(
            "_id" -> new ObjectId(taxId)),
            grater[models.Tax].asDBObject(taxRateForm),
            false,
            false)
          Redirect(controllers.backoffice.routes.TaxRates.show(
            new ObjectId(taxId))).flashing("message" -> "Tax updated !!!")
        } else {
          //println("SAVE="+taxRateForm)
          models.Tax.save(taxRateForm)
          Redirect(controllers.backoffice.routes.TaxRates.show(
            new ObjectId(taxId))).flashing("message" -> "Tax Saved !!!")
        }
      })
  }
  
  def delete(id: ObjectId) = Action { implicit request =>
    models.Tax.remove(MongoDBObject("_id" -> id))
    Redirect(controllers.backoffice.routes.TaxRates.list()).
    flashing("message" -> "Tax Class deleted !!!")
  }
  
  def getTax(id: ObjectId): models.Tax = {
    models.Tax.findOne(MongoDBObject("_id" -> id)).getOrElse(new models.Tax)
  }
}