package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._
import models._
import views._
import com.mongodb.casbah.commons.MongoDBObject
import play.api.i18n.Messages
import play.api.i18n.Lang

object ApplicationX extends Controller with Secured {

  def index = Action { implicit request =>
    Ok(views.html.index_X(Messages("home.big.label"), null))
  }

  def loginForm(lang: Lang) = Form(
    mapping(
      "email" -> email,
      "password" -> text)((email, password) =>
        UserX(email = email, password = password)) //bind    
        (user => Some(user.email, user.password))
      .verifying(Messages("customer.register.pwd.not.match")(lang), result =>
        result match {
          case UserX(_, email, password, _) =>
            UserX.findOne(MongoDBObject("email" -> email, "password"
              -> password)).isDefined
        }))

  /** Login page.
   */
  def login = Action { implicit request =>
    Ok(html.login(loginForm(lang)))
  }

  /** Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.ApplicationX.index).withNewSession.flashing(
      "message" -> Messages("customer.login.logout.msg"))
  }

  /** Handle login form submission.
   */
  def authenticate = Action { implicit request =>
    loginForm(lang).bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => Redirect(routes.ApplicationX.index)
        .withSession("email" -> user.email))
  }

}

/** Provide security features
 */
trait Secured {

  /** Retrieve the connected user email.
   */
  private def username(request: RequestHeader) = request.session.get("email")

  /** Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) =
    Results.Redirect(routes.ApplicationX.login)

  // --

  /** Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) =
    Security.Authenticated(username, onUnauthorized) {
      user => Action(request => f(user)(request))
    }

  /** Check if the connected user is a member of this project.
   */ /*
  def IsMemberOf(project: Long)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
    if(Project.isMember(project, user)) {
      f(user)(request)
    } else {
      Results.Forbidden
    }
  }*/

  /* */
  /** Check if the connected user is a owner of this task.
   */ /*
  def IsOwnerOf(task: Long)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
    if(Task.isOwner(task, user)) {
      f(user)(request)
    } else {
      Results.Forbidden
    }
  }
*/
}

