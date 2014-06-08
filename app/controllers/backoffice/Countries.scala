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

object Countries extends Controller with Secured{

  def countryForm(implicit request: RequestHeader): Form[Country] = Form(
    mapping(
      "countryId" -> nonEmptyText,
      "name" -> nonEmptyText,
      "code" -> nonEmptyText.verifying("Please enter a valid code", code => code.matches("^[a-zA-Z]{2}$")),
      "code3" -> nonEmptyText.verifying("Please enter a valid 3 digit code", code3 => code3.matches("^[a-zA-Z]{3}$")),
      "codeIsoNumeric" -> nonEmptyText.verifying("Please enter a valid 3 digit numeric ISO code", codeIsoNumeric => codeIsoNumeric.matches("^[0-9]{3}$")),
      "enabled" -> optional(boolean),
      "enabledInAA" -> optional(boolean))((
        countryId,
        name,
        code,
        code3,
        codeIsoNumeric,
        enabled,
        enabledInAA) =>
        Country(
          id = new ObjectId(countryId),
          name = name,
          code = code,
          code3 = code3,
          codeIsoNumeric = codeIsoNumeric.toInt,
          enabled = (enabled),
          enabledInAA = (enabledInAA)))(
        country => Some(
          country.id.toString,
          country.name,
          country.code,
          country.code3,
          country.codeIsoNumeric.toString,
          country.enabled,
          country.enabledInAA)))

  /** Display the paginated list of Countries.
   *
   *  @param page Current page number (starts from 0)
   *  @param orderBy Column to be sorted
   *  @param filter Filter applied on Country names
   */
  /*def list(page: Int, orderBy: Int, filter: String = "%") = Action {
    implicit request =>
      val offest = 10 * page
      var pattern = ".*(?i)" + filter + ".*";
      var countries = Country.find(MongoDBObject("name" -> pattern.r))
        .skip(offest).limit(10).toList.sortBy(c => c.name)
      var records =
        Country.find(MongoDBObject("name" -> pattern.r)).size
      Ok(views.html.backoffice.country.index(Page(countries, page, offest,
        records), orderBy, filter))
  }*/
  
  def list(page: Int, orderBy: Int, filter: String = "%") = IsAuthenticated { username =>
    implicit request =>
      val offest = 10 * page
      var pattern = ".*(?i)" + filter + ".*";
      var countries = Country.find(MongoDBObject("name" -> pattern.r))
        .skip(offest).limit(10).toList.sortBy(c => c.name)
      var records =
        Country.find(MongoDBObject("name" -> pattern.r)).size
      Admin.findOne(MongoDBObject("username" -> username)).map 
      { admin =>
        var usrToSend: Admin =
          new Admin(new ObjectId, admin.username, Option(""), admin.password,  admin.adminType)
        Ok(views.html.backoffice.country.index(Page(countries, page, offest,
        records), orderBy, filter))
      }.getOrElse(Forbidden)
  	}
  

  def show(id: ObjectId, tab: Int) = Action { implicit request =>

    val statesList = State.find(
      MongoDBObject("countryId" -> id)).toList.sortBy(c => c.name)

    Ok(views.html.backoffice.country.show(
      countryForm.fill(getCountry(id)), statesList, tab))
  }

  def save = Action { implicit request =>

    var countryId =
      request.body.asFormUrlEncoded.get("countryId").head.toString()

    val statesList = State.find(
      MongoDBObject("countryId" -> countryId)).toList

    countryForm.bindFromRequest.fold(form => {
      BadRequest(views.html.backoffice.country.show(form, statesList, 1))
    },
      countryForm => {

        if (Country.findOne(
          MongoDBObject("_id" -> new ObjectId(countryId))).isDefined) {
          Country.update(MongoDBObject(
            "_id" -> new ObjectId(countryId)),
            grater[Country].asDBObject(countryForm),
            false,
            false)
          Redirect(controllers.backoffice.routes.Countries.show(
            new ObjectId(countryId), 1)).flashing("message" -> "Country updated !!!")
        } else {
          Country.save(countryForm)
          Redirect(controllers.backoffice.routes.Countries.show(
            new ObjectId(countryId), 1)).flashing("message" -> "Country Saved !!!")
        }
      })
  }

  def delete(id: ObjectId) = Action { implicit request =>
    State.remove(MongoDBObject("countryId" -> id))
    Country.remove(MongoDBObject("_id" -> id))
    Redirect(controllers.backoffice.routes.Countries.list(0, 2, ""))
      .flashing("message" -> "Country deleted !!!")
  }

  def getCountry(id: ObjectId): Country = {
    Country.findOne(MongoDBObject("_id" -> id)).getOrElse(new Country)
  }

  /** return : String - jquery object array
   *  param  : term to search for countries
   */
  def autoComplete(term: Option[String]) = Action {
    var pattern = ".*(?i)" + term.get + ".*";
    var jsonList: String = "[{\"label\":\"* - Any country\", \"value\":\"*\" },"
    val countries = Country.find(MongoDBObject("name" -> pattern.r)).toList
    if (!countries.isEmpty) {
      var x = 0;
      for (dbObject <- countries) {
        //making json object array for jquery auto-complete
        jsonList += "{\"label\" : " + "\"" + dbObject.name + "\"," + "\"value\" : " +
          "\"" + dbObject.code + "\"," + "\"cid\" : " +
          "\"" + dbObject.id + "\"  }"
        if (x != (countries.size - 1)) {
          jsonList += ","
        }
        x = x + 1
      }
      jsonList += "]"
    } else {
      jsonList = "[{\"label\":\"* - Any country\", \"value\":\"*\" }]"
    }
    Ok(jsonList)
  }
}