package service

import play.api.Application
import securesocial.core.{UserServicePlugin, UserId, SocialUser}
import models._

import com.mongodb.casbah.Imports._
import com.novus.salat._

class MongoUserService(application: Application) extends UserServicePlugin(application) {
  
  private var users = Map[String, SocialUser]()
  
  def find(id: UserId) = {
    //users.get(id.id + id.providerId)
    models.User.findOne(MongoDBObject("social_id" -> 
    	(id.id + id.providerId).toString())).map { user =>
    	user.socialUser
    }
  }

  def save(user: SocialUser) {
    //users = users + (user.id.id + user.id.providerId -> user)
    //models.User.save(User(identifier = (user.id.id + user.id.providerId), socialUser = user))
    //println(user)
    val mongoUser = User(social_id = (user.id.id + user.id.providerId).toString(), socialUser = user, profile = Some(UserProfile()))
    User.save(mongoUser)
  }

}