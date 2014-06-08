package controllers.backoffice

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._
import models._
import views._
import com.mongodb.casbah.commons.MongoDBObject
import play.api.i18n.Messages
import play.api.i18n.Lang

object AdminLogin extends Controller with Secured {

  def index = Action { implicit request =>
    Ok(views.html.backoffice.adminlogin.login(null))
  }

  def loginForm(lang: Lang) = Form(
    mapping(
      "username" -> text,
      "password" -> text)((username, password) =>
        Admin(username = username, password = password)) //bind    
        (admin => Some(admin.username, admin.password))
      .verifying(Messages("backoffice.admin.login.err")(lang), result =>
        result match {
          case Admin(_, username, _, password, _) =>
            Admin.findOne(MongoDBObject("username" -> username, "password"
              -> password)).isDefined
        }))

  /** Login page.
   */
  def login = Action { implicit request =>
    Ok(views.html.backoffice.adminlogin.login(loginForm(lang)))
  }

  /** Logout and clean the session.
   */
  def logout = Action {
    Redirect(controllers.backoffice.routes.AdminLogin.login).withNewSession.flashing(
      "message" -> Messages("customer.login.logout.msg"))
  }

  /** Handle login form submission.
   */
  def authenticate = Action { implicit request =>
    loginForm(lang).bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.backoffice.adminlogin.login(formWithErrors)),
      admin => Redirect(controllers.backoffice.routes.AdminLogin.index)
        .withSession("username" -> admin.username))
        
  }

}

/** Provide security features
 */
trait Secured {

  /** Retrieve the connected user email.
   */
  private def username(request: RequestHeader) = request.session.get("username")

  /** Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) =
    Results.Redirect(controllers.backoffice.routes.AdminLogin.index)

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

