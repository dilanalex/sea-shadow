package models

import scala.collection.mutable.MutableList
import play.api.Play.current

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._

case class CustomerGroup(

  id: ObjectId = new ObjectId,
  var name: String = "",
  var description: String = "",
  var disableTaxInclusive: Option[Boolean] = None,
  var taxExempt: Option[Boolean] = None) {

}

object CustomerGroup extends ModelCompanion[CustomerGroup, ObjectId] {
  val collection = mongoCollection("CustomerGroups")
  val dao = new SalatDAO[CustomerGroup, ObjectId](collection = collection) {}  
  val customerGroups = CustomerGroup.findAll.toList.sortBy(cg => cg.name)
}