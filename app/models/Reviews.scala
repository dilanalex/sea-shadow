package models

import play.api.Play.current
import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.salatctx._
import java.util.Date

case class Reviews(
  id: ObjectId = new ObjectId,
  var product_id: ObjectId,
  var date: Date = new Date(),
  var title: String = "",
  var text: String = "",
  var rating: Int = 0,
  var userId: ObjectId,
  var username: String = "",
  var helpfulVotes: Int = 0,
  var voterIds: List[ObjectId] = List()
)

object Reviews extends ModelCompanion[Reviews, ObjectId] {
  val collection = mongoCollection("reviews")
  val dao = new SalatDAO[Reviews, ObjectId](collection = collection) {}
}