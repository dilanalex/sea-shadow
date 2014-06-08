package helpers

import play.api.Play.current

import models.Taxonomy
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject

object Taxonomies {

  def taxonomy(id: ObjectId): Taxonomy = Taxonomy.findOneById(id).get
  def taxonomy(path: String): Taxonomy =
    Taxonomy.findOne(MongoDBObject("path" -> path)).get

  private def executePathQuery(path: String) = {
    Taxonomy.find(MongoDBObject("path" -> path.r))
      .sort(MongoDBObject("path" -> 1))
  }

  def children(taxonomy: Taxonomy) = {
    if (taxonomy == null) {
      Taxonomy.find(MongoDBObject("parent" -> null))
    } else {
      executePathQuery("^" + taxonomy.path + "/(?!.*(?:/))")
    }
  }

  def descendents(taxonomy: Taxonomy) = {
    if (taxonomy == null) {
      Taxonomy.find(MongoDBObject("parent" -> null))
    } else {
      executePathQuery("^" + taxonomy.path + "/")
    }
  }

  def breadcrumbs(taxonomy: Taxonomy) = {
    if (taxonomy == null) {
      List.empty
    } else {
      Taxonomy.find("_id" $in taxonomy.ancestors)
        .sort(MongoDBObject("path" -> 1))
    }
  }

}
