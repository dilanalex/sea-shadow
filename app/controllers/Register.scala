package controllers
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import play.api._
import com.mongodb.casbah.commons.MongoDBObject
import models._
import play.api.i18n.Messages
import play.api.i18n.Lang

object Register extends Controller {
  def registrationForm(lang: Lang) = Form(
    mapping(
      "email" -> email,
      "password" ->
        tuple("main" -> text, "confirm" -> text)
        .verifying(Messages("customer.register.pwd.not.match")(lang),
          passwords => passwords._1 == passwords._2))((email, passwords) =>
        UserX(email = email, password = passwords._1,
          profile = Some(new UserProfileX)))(user =>
        Some(user.email, (user.password, ""))))

  def index = Action { implicit request =>
    Ok(views.html.register(registrationForm(lang)))
  }

  def register = Action { implicit request =>  
    registrationForm(lang).bindFromRequest.fold(
      form => BadRequest(views.html.register(form)),
      registration => {
        try {
          UserX.save(registration)
          Redirect(routes.Register.index())
            .flashing("message" -> Messages("customer.register.success"))
        } catch {
          case e: com.mongodb.MongoException.DuplicateKey => {
            Redirect(routes.Register.index())
              .flashing("errormessage" -> Messages("customer.register.exist"))
          }
        }
      })
  }

}