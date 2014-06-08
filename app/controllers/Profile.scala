package controllers
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.DBObject
import com.novus.salat.global._
import com.novus.salat._
import models._
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import play.api._
import com.mongodb.casbah.Imports._
import play.api.i18n.Messages
import securesocial.core.{SocialUser}
import securesocial.core.SecureSocial
import securesocial.core.SocialUser
import securesocial.core.providers.UsernamePasswordProvider
import org.bson.types.ObjectId
import securesocial.core.UserId
import securesocial.core.AuthenticationMethod
import securesocial.core.PasswordInfo
import securesocial.core.providers.utils.PasswordHasher
import com.typesafe.plugin._
import Play.current
import models.salatctx._

object Profile extends Controller with SecureSocial {

  val providerId = UsernamePasswordProvider.UsernamePassword
  case class SocialInfo(email: String)
  
  def profileForm(implicit request: RequestHeader) = Form(
    mapping(
    "id" -> text,
    "social_id" -> text,
    "socialUser" -> mapping(        
        "idSS" -> mapping(
            "id" -> text,
            "providerId" -> text
          ) (UserId.apply)(UserId.unapply),
        "displayName" -> text,
        "authMethod"  -> mapping(
            "method" -> text            
          )(AuthenticationMethod.apply)(AuthenticationMethod.unapply),
        "isEmailVerified" -> text,
        "email" -> text,
        "avatarUrl" -> text,
        "passwordInfo" -> mapping(
            "password" -> tuple(
            	"main" -> text,
            	"confirm" -> text,
            	"old" -> text).verifying(Messages("customer.profile.pwd.not.match"), passwords =>
          if (passwords._3 != "") {
            //println("XXXXXXXXXXX"+passwords._3)
            val user = User.findOne(
                MongoDBObject("social_id"->{request.session.get("securesocial.user").get+
                							request.session.get("securesocial.provider").get})
                					).get           
            use[PasswordHasher].matches(user.socialUser.passwordInfo.get, passwords._3)
          } else {
            //println("YYYYYYYYYYYY"+passwords._3)
            true
          }).verifying(Messages("customer.profile.newpwd.not.match"), passwords =>
          if (passwords._1 != "" || passwords._2 != "") {
            //println("AAAAAAAAAAAAA"+passwords._3)
            passwords._1 == passwords._2           
          } else {
            //println("BBBBBBBBBBBBBBBB"+passwords._3)
            true
          }).verifying(Messages("Please enter your old password to change the password"), passwords =>
          if (passwords._1 != "" && passwords._1 == passwords._2 && passwords._3 == "") {
            //println("CCCCCCCCCCCC"+passwords._3)
            false           
          } else {
            //println("DDDDDDDDDD"+passwords._3)            
            true
          })
        )
        ((passwords) => {
          if (passwords._2 != "" && passwords._3 != "") {
        	  PasswordInfo(password = passwords._2)
            } else {
              val loggedinUser = User.findOne(
                  MongoDBObject("social_id"->{request.session.get("securesocial.user").get+
                	  						  request.session.get("securesocial.provider").get})
                	  			 			  ).get
              PasswordInfo(password = loggedinUser.socialUser.passwordInfo.get.password)//NO_CHANGE password
            }
          } 
        )
        (pw => Some(pw.password, "", ""))
    )
    ((idSS, displayName, authMethod, isEmailVerified, email, avatarUrl, passwordInfo) => 
      SocialUser(id = idSS, displayName = displayName, authMethod = authMethod, isEmailVerified = isEmailVerified.toBoolean,
                 email = Some(email), avatarUrl = Some(avatarUrl), passwordInfo = Some(passwordInfo)))
    (user => Some(user.id, user.displayName,user.authMethod, user.isEmailVerified.toString ,user.email.getOrElse(""), 
                  user.avatarUrl.getOrElse(""), user.passwordInfo.get))
     ,                  
      // Create a mapping that will handle UserProfile values
      "profile" -> optional(mapping(
        "firstName" -> optional(text),
        "lastName" -> optional(text),
        "activeDeliveryAddress" -> optional(mapping(
          "firstName" -> optional(text),
          "lastName" -> optional(text),
          "addLine1" -> optional(text),
          "addLine2" -> optional(text),
          "city" -> optional(text),
          "postCode" -> optional(text),
          "country" -> optional(text)) {
            (firstName,
            lastName,
            addLine1,
            addLine2,
            city,
            postCode,
            country) =>
              DeliveryAddress(
                firstName,
                lastName,
                addLine1,
                addLine2,
                city,
                postCode,
                country)
          } {
            activeDeliveryAddress =>
              Some((activeDeliveryAddress.firstName,
                activeDeliveryAddress.lastName,
                activeDeliveryAddress.addLine1,
                activeDeliveryAddress.addLine2,
                activeDeliveryAddress.city,
                activeDeliveryAddress.postCode,
                activeDeliveryAddress.country))
          }),
        "activeBillingAddress" -> optional(mapping(
          "addLine1" -> optional(text),
          "addLine2" -> optional(text),
          "city" -> optional(text),
          "postCode" -> optional(text),
          "country" -> optional(text)) {
            (addLine1, addLine2, city, postCode, country) =>
              BillingAddress(addLine1, addLine2, city, postCode, country)
          } {
            activeBillingAddress =>
              Some((activeBillingAddress.addLine1,
                activeBillingAddress.addLine2,
                activeBillingAddress.city,
                activeBillingAddress.postCode,
                activeBillingAddress.country))
          })) // The mapping signature matches the UserProfile case class 
          //signature, so we can use default apply/unapply functions here
          {
          (firstName, lastName, activeDeliveryAddress, activeBillingAddress) =>
            UserProfile(
              firstName = firstName,
              lastName = lastName,
              activeDeliveryAddress = if (activeDeliveryAddress.isDefined) {
                activeDeliveryAddress
              } else {
                Some(new DeliveryAddress())
              }, activeBillingAddress = if (activeBillingAddress.isDefined) {
                activeBillingAddress
              } else {
                Some(new BillingAddress())
              })
        } {
          profile =>
            Some(profile.firstName,
              profile.lastName,
              profile.activeDeliveryAddress,
              profile.activeBillingAddress)
        })
    )
    ((id, social_id, socialUser, profile) => User(id = new ObjectId(id), social_id = social_id, socialUser = socialUser, profile = profile))
    (user => Some(user.id.toString(), user.social_id, user.socialUser, user.profile))
    
  )
  
  def viewUsr = SecuredAction() { 
    implicit request =>
      
      User.findOne(MongoDBObject("socialUser.email" -> request.user.email.get)).map 
      { user =>
        val ssuser = SocialUser(request.user.id, request.user.displayName,
          request.user.email,
          request.user.avatarUrl,
          request.user.authMethod,          
          passwordInfo  = Some(PasswordInfo("")) //display a blank password even though it binds to the field
          )
         
        var usrToSend = User(id = user.id, social_id = user.social_id, socialUser = ssuser, profile = user.profile)
        println ("user.id="+user.id)
        Ok(views.html.profile(profileForm.fill(usrToSend), request.user))
      }.getOrElse(Forbidden)
  }
  
  def updateUsers = SecuredAction() {    
    implicit request =>      
        profileForm.bindFromRequest.fold(
          form => {println("BAD REQUEST"); BadRequest(views.html.profile(form, request.user))},
           profileF => {           
            //println("pw="+profileF.socialUser.passwordInfo.get.password)
            //println("profileF="+profileF)           
            
            val user = User.findOne(MongoDBObject("social_id" -> profileF.social_id)).get
            
            
            if (profileF.profile.isDefined) {
                        
              profileF.profile.get.deliveryAddresses ++=
                user.profile.get.deliveryAddresses
              profileF.profile.get.billingAddresses ++=
                user.profile.get.billingAddresses
              
              if (!user.profile.get.activeDeliveryAddress
                .equals(profileF.profile.get.activeDeliveryAddress)) {
                profileF.profile.get.deliveryAddresses ::=
                  profileF.profile.get.activeDeliveryAddress.get
              }
              if (!user.profile.get.activeBillingAddress
                .equals(profileF.profile.get.activeBillingAddress)) {
                profileF.profile.get.billingAddresses ::=
                  profileF.profile.get.activeBillingAddress.get
              }
              
              User.update(MongoDBObject("social_id" -> profileF.social_id), //Find by social_id
                 grater[User].asDBObject(profileF
                     ), false, false)
            } else {
              profileF.profile = Some(UserProfile())
              User.update(MongoDBObject("social_id" -> profileF.social_id),
                grater[User].asDBObject(profileF), false, false)
            }
                        
            if(profileF.socialUser.passwordInfo.get.password.indexOf("$2a$10") < 0){ // <-- password changed
              println("IS SAVING PASSWORD")
              User.update(MongoDBObject("social_id" -> profileF.social_id), //Find by social_id
                 $set("socialUser.passwordInfo.password" -> 
                 		use[PasswordHasher].hash(profileF.socialUser.passwordInfo.get.password).password //Encrypt the pw and save
                     ), false, false)                    
            } 
                        
            
            //UserS.update(MongoDBObject("social_id" -> profileF.social_id), grater[UserS].asDBObject(profileF), false, false)              
            Redirect(routes.Profile.viewUsr()).flashing("message" -> Messages("customer.profile.user.update"))
          })
      
  }
  
  def saveDeliveryAddr(user: User, delAddr: DeliveryAddress) = {
    if (user.profile.get.deliveryAddresses.isEmpty) {
      user.addDeliveryAddresses(delAddr)
    } else {
      if (!user.profile.get.deliveryAddresses.head.equals(delAddr))
        user.addDeliveryAddresses(delAddr)
    }
    User.save(user)
  }

  def saveBillingAddr(user: User, bilAddr: BillingAddress) = {
    if (user.profile.get.billingAddresses.isEmpty) {
      user.addBillingAddresses(bilAddr)
    } else {
      if (!user.profile.get.billingAddresses.head.equals(bilAddr))
        user.addBillingAddresses(bilAddr)
    }
    User.save(user)
  }

}
