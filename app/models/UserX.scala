package models


import play.api.Play.current
import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._

case class UserProfileX(
  var firstName: Option[String] = None,
  var lastName: Option[String] = None,
  var activeDeliveryAddress: Option[DeliveryAddressX] = None,
  var deliveryAddresses: List[DeliveryAddressX] = Nil,
  var activeBillingAddress: Option[BillingAddressX] = None,
  var billingAddresses: List[BillingAddressX] = Nil
)  
  
case class DeliveryAddressX(
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  addLine1: Option[String] = None,
  addLine2: Option[String] = None,
  city: Option[String] = None,
  postCode: Option[String] = None,
  country: Option[String] = None
)

case class BillingAddressX(
  addLine1: Option[String] = None,
  addLine2: Option[String] = None,
  city: Option[String] = None,
  postCode: Option[String] = None,
  country: Option[String] = None
)

case class UserX(
  id: ObjectId = new ObjectId,
  email: String,
  var password: String,
  var profile: Option[UserProfileX] = None
)
{  
  def addDeliveryAddresses(addr: DeliveryAddressX) {        
    profile.get.deliveryAddresses ::= addr
    profile.get.activeDeliveryAddress = Some(addr)   
  }  

  def addBillingAddresses(addr: BillingAddressX) {        
    profile.get.billingAddresses ::= addr
    profile.get.activeBillingAddress = Some(addr)   
  }  
}

object UserX extends ModelCompanion[UserX, ObjectId] {
  val collection = mongoCollection("userX")
  collection.ensureIndex(MongoDBObject("email" -> 1), "email_index", true)
  val dao = new SalatDAO[UserX, ObjectId](collection = collection) {}  
}
  

