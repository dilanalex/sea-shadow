package controllers.backoffice

import play.api.data.Form
import play.api.data.Forms._
import models._
import com.mongodb.casbah.Imports._
import play.api.mvc._
import com.mongodb.casbah.Imports._
import models.salatctx._
import com.novus.salat._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ListBuffer

object States extends Controller {

  def stateForm(implicit request: RequestHeader): Form[State] = Form(
    mapping(
      "stateId" -> nonEmptyText,
      "countryId" -> nonEmptyText,
      "code" -> optional(text),
      "name" -> optional(text))((
        stateId,
        countryId,
        code,
        name) =>
        State(
          id = new ObjectId(stateId),
          countryId = new ObjectId(countryId),
          code = code,
          name = name))(
        state =>
          Some(state.id.toString,
            state.countryId.toString(),
            state.code,
            state.name)))

  def show(id: ObjectId, countryId: ObjectId) = Action { implicit request =>
    Ok(views.html.backoffice.state.show(stateForm.fill(getState(id)), countryId))
  }

  def save = Action { implicit request =>

    var stateId =
      request.body.asFormUrlEncoded.get("stateId").head.toString()
    var countryId =
      new ObjectId(request.body.asFormUrlEncoded.get("countryId").head.toString())

    stateForm.bindFromRequest.fold(form => {
      BadRequest(views.html.backoffice.state.show(form, countryId))
    },
      stateForm => {

        if (State.findOne(
          MongoDBObject("_id" -> new ObjectId(stateId))).isDefined) {

          State.update(MongoDBObject("_id" ->
            new ObjectId(stateId)), grater[State].asDBObject(stateForm), false, false)

          Redirect(controllers.backoffice.routes.States.show(
            new ObjectId(stateId), countryId))
            .flashing("message" -> "State updated !!!")
        } else {
          State.save(stateForm)
          Redirect(controllers.backoffice.routes.States.show(
            new ObjectId(), countryId)).flashing("message" -> "State Saved !!!")
        }
      })
  }

  def delete(id: ObjectId, countryId: ObjectId) = Action { implicit request =>
    State.remove(MongoDBObject("_id" -> id))
    Redirect(controllers.backoffice.routes.Countries.show(countryId, 2))
      .flashing("message" -> "State deleted !!!")
  }

  def getState(id: ObjectId): State = {
    State.findOne(MongoDBObject("_id" -> id)).getOrElse(new State)
  }
  
  def getCountryStateList(countryCode: String) = Action { implicit request =>
    val contry=Country.findOne(MongoDBObject("code" -> countryCode))
    val states = State.find(MongoDBObject("countryId" -> contry.get.id)).toList.sortBy(c => c.name)    
    var stateJSON: String="{\"states\":[" 
    states.foreach(f => stateJSON = stateJSON + grater[State].toPrettyJSON(f).toString() + ",")
    stateJSON = stateJSON+"]}" 
    Ok(stateJSON)    
  }
  
   /**
   * return : String - jquery object array
   * param  : term : to search for states
   *          cid  : country id
   */
  def autoCompleteState(term: Option[String], cid: Option[String]) = Action { 
    var pattern = ".*(?i)" + term.get + ".*";
    val c = Country.findOne(MongoDBObject("code" -> cid))
    val countries = State.find(MongoDBObject("countryId" -> 
    		new ObjectId(c.get.id.toString()), "name" -> pattern.r)).toList
    var jsonList : String = "[{\"label\":\"* - Any state\", \"value\":\"*\" },"		
    if(!countries.isEmpty){		
	    var x = 0;
	    for (dbObject <- countries) {
	      //making json object array for jquery auto-complete
	      jsonList +=  "{\"label\":" + "\""  +dbObject.name.get+"\"," + "\"value\":"+
	      "\""+dbObject.code.get+"\"," + "\"cid\" : " + "\""+dbObject.id+"\"  }"
	      if(x != (countries.size-1)){
	        jsonList += ","
	      }
	      x=x+1
	    }
	    jsonList += "]"
    }else{
      jsonList = "[{\"label\":\"* - Any state\", \"value\":\"*\" }]"
    }
    Ok(jsonList)
  }

}