package controllers.backoffice

import play.api._
import play.api.mvc._
import com.mongodb.casbah.Imports._
import com.novus.salat._
import models.salatctx._
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

object Currencies extends Controller with Secured{

  def currencyForm(implicit request: RequestHeader): Form[Currency] = Form(
    mapping(
      "currencyId" -> nonEmptyText,
      "iso" -> nonEmptyText
        .verifying("Please valid unique 3 digit ISO code",
          iso => iso.matches("^[a-zA-Z]{3}$")),
      "name" -> nonEmptyText,
      "symbol" -> nonEmptyText,
      "rate" -> nonEmptyText
        .verifying("Please enter a valid decimal value for rate",
          rate => rate.matches("""^[0-9]\d*(\.\d+)?$""")),
      "enabled" -> optional(boolean),
      "baseCurrency" -> optional(boolean))((
        currencyId,
        iso,
        name,
        symbol,
        rate,
        enabled,
        baseCurrency) =>
        Currency(
          id = new ObjectId(currencyId),
          iso = iso,
          name = name,
          symbol = symbol,
          rate = rate.toDouble,
          enabled = (enabled),
          baseCurrency = (baseCurrency)))(
        currency => Some(
          currency.id.toString,
          currency.iso,
          currency.name,
          currency.symbol,
          currency.rate.toString,
          currency.enabled,
          currency.baseCurrency)))

  /*def list() = Action { implicit request =>
    val currencies = Currency.findAll().toList.sortBy(c => c.name)
    Ok(views.html.backoffice.currency.list(currencies))
  }*/
  
  def list = IsAuthenticated { username =>
    implicit request =>
      val currencies = Currency.findAll().toList.sortBy(c => c.name)
      Admin.findOne(MongoDBObject("username" -> username)).map 
      { admin =>
        var usrToSend: Admin =
          new Admin(new ObjectId, admin.username, Option(""), admin.password, admin.adminType)
        Ok(views.html.backoffice.currency.list(currencies))
      }.getOrElse(Forbidden)
  	}

  def show(id: ObjectId) = Action { implicit request =>

    Ok(views.html.backoffice.currency.show(currencyForm.fill(getCurrency(id))))

  }

  def save = Action { implicit request =>

    val currencyId = request.body.asFormUrlEncoded.get("currencyId").head.toString()

    currencyForm.bindFromRequest.fold(form => {
      BadRequest(views.html.backoffice.currency.show(form))
    },
      currencyForm => {

        val currency = Currency.findOne(MongoDBObject(
          "_id" -> new ObjectId(currencyId)))

        if (currency.isDefined) {

          if (!currency.get.iso.equals(currencyForm.iso)
            && Currency.findOne(MongoDBObject("iso" -> currencyForm.iso)).isDefined) {
            Redirect(controllers.backoffice.routes.Currencies.show(
              new ObjectId(currencyId))).flashing("errormessage"
              -> Messages( "backoffice.currency.isocode.exists" ))

          } else {

            Currency.update(MongoDBObject(
              "_id" -> new ObjectId(currencyId)),
              grater[Currency].asDBObject(changeBaseCurrency(currencyForm)),
              false,
              false)

            Redirect(controllers.backoffice.routes.Currencies.show(
              new ObjectId(currencyId))).flashing("message" -> Messages( "backoffice.currency.updated" ))
          }

        } else if (Currency.findOne(MongoDBObject("iso" -> currencyForm.iso)).isDefined) {

          Redirect(controllers.backoffice.routes.Currencies.show(
            new ObjectId(currencyId))).flashing("errormessage"
            -> Messages( "backoffice.currency.isocode.exists" ))

        } else {

          Currency.save(changeBaseCurrency(currencyForm))
          Redirect(controllers.backoffice.routes.Currencies.show(
            new ObjectId())).flashing("message" -> Messages( "backoffice.currency.saved" ))

        }
      })

  }

  private def changeBaseCurrency(
    currencyForm: Currency): Currency = {

    var oldBaseCurrency =
      Currency.findOne(MongoDBObject("enabled" -> ("true".toBoolean),
        "baseCurrency" -> ("true".toBoolean))).getOrElse(null)

    if (currencyForm.baseCurrency.isDefined
      && currencyForm.baseCurrency.get == true) {

      if (oldBaseCurrency != null &&
        !(currencyForm.id.toString().equals(oldBaseCurrency.id.toString()))) {

        oldBaseCurrency.baseCurrency = null
        Currency.save(oldBaseCurrency)

      }

    }

    return currencyForm
  }

  def delete(id: ObjectId) = Action { implicit request =>
    val currency = getCurrency(id)

    if (currency.baseCurrency.isDefined
      && currency.baseCurrency.get == true) {

      Redirect(controllers.backoffice.routes.Currencies.show(id))
        .flashing("errormessage" ->
          Messages( "backoffice.currency.basecur.err" ))
    } else {

      Currency.remove(MongoDBObject("_id" -> id))

      Redirect(controllers.backoffice.routes.Currencies.list())
        .flashing("message" -> Messages( "backoffice.currency.deleted" ))
    }
  }

  private def getCurrency(id: ObjectId): Currency = {
    Currency.findOne(MongoDBObject("_id" -> id)).getOrElse(new Currency)
  }
  
  def setCurrency(iso: String) = Action { implicit request =>
    val cur = Currency.findOne(MongoDBObject("enabled" -> ("true".toBoolean),
    										 "iso" -> iso)).getOrElse(null)
    val baseCurrency = Currency.baseCurrency
    Ok(iso).withSession(session+
        ("setCurrency" -> iso)+
        ("cSymbol" -> cur.symbol)+
        ("cRate" -> cur.rate.toString)+
        ("bCSymbol" -> baseCurrency.symbol)+
        ("cCode" -> cur.iso)
      )
  }

}
