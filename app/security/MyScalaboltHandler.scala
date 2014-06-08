package security

import be.objectify.deadbolt.models.RoleHolder
import models.SecurityUser
import be.objectify.deadbolt.scalabolt.{DynamicResourceHandler, ScalaboltHandler}
import play.api.mvc.{Result, Results}
import play.mvc.Http.Context
import play.api.Play
import models.salatctx._


/**
 *
 * @author Steve Chaloner (steve@objectify.be)
 */
class MyScalaboltHandler(dynamicResourceHandler: DynamicResourceHandler = null) extends ScalaboltHandler
{
  override def getDynamicResourceHandler: DynamicResourceHandler =
  {
    if (dynamicResourceHandler != null)
    {
      dynamicResourceHandler
    }
    else
    {
      new MyDynamicResourceHandler()
    };
  }

  override def getRoleHolder: RoleHolder =
  {
    //val user = Context.current().session().get("user")
    //Session sessions = play.mvc.Http.Context.current().session()
    //val user= Context.current().session().get("user")    
    //println("USER="+user)
    new SecurityUser("steve")
    
  }

  def onAccessFailure: Result =
  {
    Results.Forbidden(views.html.accessFailed())
  }
}