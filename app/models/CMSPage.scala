package models

import play.api.Play.current

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._

import models.salatctx._


case class CMSPage(
  id: ObjectId = new ObjectId,
  var title: String = "",
  var url: String = "",
  var content: String = "",
  var published: Option[Boolean] = None,
  var label: Option[String] = None
)

object CMSPage extends ModelCompanion[CMSPage, ObjectId] {
  val collection = mongoCollection("staticpages")
  val dao = new SalatDAO[CMSPage, ObjectId](collection = collection) {}
}