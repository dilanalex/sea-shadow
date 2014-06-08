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

object ProfileX extends Controller with Secured {

  def profileFormX(implicit request: RequestHeader) = Form(
    mapping(
      "email" -> email,
      // Create a tuple mapping for the password/confirm
      "password" -> tuple(
        "main" -> text,
        "confirm" -> text,
        "old" -> text).verifying(Messages("customer.profile.pwd.not.match"), passwords =>
          if (passwords._3 != "") {
            UserX.findOne(MongoDBObject("email" ->
              request.session.get("email"),
              "password" -> passwords._3)).isDefined
          } else {
            true
          }).verifying(Messages("customer.profile.newpwd.not.match"), passwords =>
          if (passwords._3 != "") {
            passwords._1 == passwords._2
          } else {
            true
          }),
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
              DeliveryAddressX(
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
              BillingAddressX(addLine1, addLine2, city, postCode, country)
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
            UserProfileX(
              firstName = firstName,
              lastName = lastName,
              activeDeliveryAddress = if (activeDeliveryAddress.isDefined) {
                activeDeliveryAddress
              } else {
                Some(new DeliveryAddressX())
              }, activeBillingAddress = if (activeBillingAddress.isDefined) {
                activeBillingAddress
              } else {
                Some(new BillingAddressX())
              })
        } {
          profile =>
            Some(profile.firstName,
              profile.lastName,
              profile.activeDeliveryAddress,
              profile.activeBillingAddress)
        })) // The mapping signature doesn't match the User case class 
        // signature,so we have to define custom binding/unbinding functions
        {
        // Binding: Create a User from the mapping result (ignore the second
        //      password and the accept field)
        (email, passwords, profile) =>
          {
            if (passwords._2 != "" && passwords._3 != "") {
              UserX(email = email,
                password = passwords._2,
                profile = profile)
            } else {
              var loggedinUser = UserX.findOne(MongoDBObject("email" ->
                request.session.get("email")))
              UserX(email = email,
                password = loggedinUser.get.password, profile = profile)
            }
          }
      } {
        // Unbinding: Create the mapping values from an existing User value
        user => Some(user.email, (user.password, "", ""), user.profile)
      })

  def view = IsAuthenticated { username =>
    implicit request =>
      UserX.findOne(MongoDBObject("email" -> username)).map 
      { user =>
        var usrToSend: UserX =
          new UserX(new ObjectId, user.email, "", user.profile)
        Ok(views.html.profileX(profileFormX.fill(usrToSend), user))
      }.getOrElse(Forbidden)
  }

  def updateUser = IsAuthenticated { username =>
    implicit request =>
      UserX.findOne(MongoDBObject("email" -> username)).map { user =>
        profileFormX.bindFromRequest.fold(
          form => BadRequest(views.html.profileX(form, user)),
          profileF => {
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
              UserX.update(MongoDBObject("email" -> profileF.email),
                grater[UserX].asDBObject(profileF), false, false)
            } else {
              profileF.profile = Some(UserProfileX())
              UserX.update(MongoDBObject("email" -> profileF.email),
                grater[UserX].asDBObject(profileF), false, false)
            }
            Redirect(routes.ProfileX.view())
              .flashing("message" -> Messages("customer.profile.user.update"))
          })
      }.getOrElse(Forbidden)
  }

  def saveDeliveryAddr(user: UserX, delAddr: DeliveryAddressX) = {
    if (user.profile.get.deliveryAddresses.isEmpty) {
      user.addDeliveryAddresses(delAddr)
    } else {
      if (!user.profile.get.deliveryAddresses.head.equals(delAddr))
        user.addDeliveryAddresses(delAddr)
    }
    UserX.save(user)
  }

  def saveBillingAddr(user: UserX, bilAddr: BillingAddressX) = {
    if (user.profile.get.billingAddresses.isEmpty) {
      user.addBillingAddresses(bilAddr)
    } else {
      if (!user.profile.get.billingAddresses.head.equals(bilAddr))
        user.addBillingAddresses(bilAddr)
    }
    UserX.save(user)
  }

}
