package models

import be.objectify.deadbolt.models.RoleHolder
import play.libs.Scala

/**
 *
 * @author Steve Chaloner (steve@objectify.be)
 */

class SecurityUser(val userName: String) extends RoleHolder
{
  
  println("userNameXXXXXXXXXXXXXXXX="+userName)
  def getRoles: java.util.List[SecurityRole] = {
    Scala.asJava(List(new SecurityRole("foo"),
                      new SecurityRole("bar")))
  }

  def getPermissions: java.util.List[UserPermission] = {
    Scala.asJava(List(new UserPermission("printers.edit")))
  }
}
