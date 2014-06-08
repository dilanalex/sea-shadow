package models

import play.api.Play.current
import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._
import securesocial.core.{ SocialUser }
import controllers.Profile.SocialInfo

case class UserProfile(
  var firstName: Option[String] = None,
  var lastName: Option[String] = None,
  var activeDeliveryAddress: Option[DeliveryAddress] = None,
  var deliveryAddresses: List[DeliveryAddress] = Nil,
  var activeBillingAddress: Option[BillingAddress] = None,
  var billingAddresses: List[BillingAddress] = Nil)

case class DeliveryAddress(
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  addLine1: Option[String] = None,
  addLine2: Option[String] = None,
  city: Option[String] = None,
  postCode: Option[String] = None,
  country: Option[String] = None,
  state: Option[String] = None)

case class BillingAddress(
  addLine1: Option[String] = None,
  addLine2: Option[String] = None,
  city: Option[String] = None,
  postCode: Option[String] = None,
  country: Option[String] = None,
  state: Option[String] = None)

case class User(
  id: ObjectId = new ObjectId,
  social_id: String,
  socialUser: SocialUser,
  var profile: Option[UserProfile] = None) {
  def addDeliveryAddresses(addr: DeliveryAddress) {
    profile.get.deliveryAddresses ::= addr
    profile.get.activeDeliveryAddress = Some(addr)
  }

  def addBillingAddresses(addr: BillingAddress) {
    profile.get.billingAddresses ::= addr
    profile.get.activeBillingAddress = Some(addr)
  }
}

object User extends ModelCompanion[User, ObjectId] {
  val collection = mongoCollection("users")
  collection.ensureIndex(MongoDBObject("social_id" -> 1), "social_id_index", true)
  val dao = new SalatDAO[User, ObjectId](collection = collection) {}
}
