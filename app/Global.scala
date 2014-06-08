

import com.mongodb.casbah.Imports._
import play.api._
import models._
import se.radley.plugin.salat._
import com.mongodb.util.JSON
import scala.io.Source
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.global._
import com.novus.salat._
import com.traackr.scalastic.elasticsearch.Indexer
import play.api.Play
import java.util.Date
import models.State
import models.Country
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.Set
import controllers.backoffice.Currencies

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    //creating MongoDB + ElasticSearch River index
    try {
      val indexer = Indexer.transport(settings =
        Map(("cluster.name", Play.current.configuration
          .getString("elasticsearch.cluster.name")
          .getOrElse("elasticsearch"))),
        host = Play.current.configuration
          .getString("elasticsearch.mongodb.host")
          .getOrElse("localhost"))

      indexer.index("_river", "mongodb", "_meta",
        """{
          "type": "mongodb", 
          "mongodb": { 
            "db":"""" + Play.current.configuration
          .getString("elasticsearch.mongodb.db")
          .getOrElse("obcdb") + """","collection": """"
          + Play.current.configuration
          .getString("elasticsearch.mongodb.product.collection")
          .getOrElse("products") + """","gridfs": true
          }, 
          "index": { 
              "name": """" + Play.current.configuration
          .getString("elasticsearch.mongodb.productindex.name")
          .getOrElse("productindex") + """",
              "type":  """" + Play.current.configuration
          .getString(
            "elasticsearch.mongodb.productindex.type")
          .getOrElse("product") + """" 
          }
       }""")
      indexer.refresh()
      indexer.stop()
    } catch {
      case exception:
        org.elasticsearch.client.transport.NoNodeAvailableException =>
        println("ES down")
      case allotherExceptions => println(allotherExceptions.getMessage())
    }
    // Do we want to load new products and categories?
    // - take our lead from products
    if (Product.collection.size == 0) {
      // Load in the products
      val pDocs = JSON.parse(
        Source.fromFile(app.path + "/conf/data/products.json") mkString)
        .asInstanceOf[DBObject]

      pDocs.foreach(item => {
        Product.collection.insert(item._2.asInstanceOf[DBObject])
      })

      // Now empty and load the categories
      Taxonomy.collection.remove(MongoDBObject())
      // And load them
      val cDocs = JSON.parse(
        Source.fromFile(app.path + "/conf/data/categories.json") mkString)
        .asInstanceOf[DBObject]

      cDocs.foreach(item => {
        Taxonomy.collection.insert(item._2.asInstanceOf[DBObject])
      })
    }

    if (Country.collection.size == 0) {
      // Load in the products
      val pDocs = JSON.parse(
        Source.fromFile(app.path + "/conf/data/countries.json") mkString)
        .asInstanceOf[DBObject]

      pDocs.foreach(item => {
        Country.collection.insert(item._2.asInstanceOf[DBObject])

      })

    }
    if (State.collection.size == 0) {
      // Load in the products
      val pDocs = JSON.parse(
        Source.fromFile(app.path + "/conf/data/states.json") mkString)
        .asInstanceOf[DBObject]

      pDocs.foreach(item => {
        State.collection.insert(item._2.asInstanceOf[DBObject])
      })

    }

    if (CustomerGroup.collection.size == 0) {
      // Load in the Customer groups
      val pDocs = JSON.parse(
        Source.fromFile(app.path + "/conf/data/CustomerGroups.json") mkString)
        .asInstanceOf[DBObject]

      pDocs.foreach(item => {
        CustomerGroup.collection.insert(item._2.asInstanceOf[DBObject])
      })

    }

    if (Currency.collection.size == 0) {
      // Load in the Customer groups
      val pDocs = JSON.parse(
        Source.fromFile(app.path + "/conf/data/Currencies.json") mkString)
        .asInstanceOf[DBObject]

      pDocs.foreach(item => {
        Currency.collection.insert(item._2.asInstanceOf[DBObject])
      })

    }

    // "_id" $in product.relatedProducts
    //insert dummy data to the review table : just for testing
    //Later this should be moved to JSON file  
    if (Reviews.collection.size < 30) {
      Product.collection.foreach(product =>
        Reviews.save(new models.Reviews(
          id = new ObjectId(),
          product_id = new ObjectId(product.asInstanceOf[DBObject]
            .get("_id").toString()),
          date = new Date(),
          title = "I'm really happy with this product.",
          text = "I bought this product " + product.asInstanceOf[DBObject]
            .get("description"),
          rating = 4,
          userId = new ObjectId("50179a7e9bde63537d0e49d7"),
          username = "nimal@abc.com",
          helpfulVotes = 3,
          voterIds = List(new ObjectId("50179a7e9bde63537d0e49d7"),
            new ObjectId("50179a7e9bde63537d0e49d7")))))
    }
  }

  /*
   * Method not in use, but saved for it's future potential for being able
   *  to relate docs by ObjectId, when the ObjectId is not known in advance 
   *  of import really just template code to be used in the future or not
   */

  def complicatedRelatedLoading(dataDir: String) = {

    /*
     * Json looks like this
     * {
     *   "_id":0,
     *   "name":"Mens",
     *   "path":"mens"
     * },
	 * {
	 *   "_id":2,
	 *   "name":"Bottoms",
	 *   "path":"mens/bottoms",
	 *   "parent":0,
	 *   "ancestors": [0]
	 * },
	 * {
	 *   "_id":3,
	 *   "name":"Shorts",
	 *   "path":"mens/bottoms/shorts",
	 *   "parent":2,
	 *   "ancestors": [2, 0]
	 * }
     */

    val docs = JSON.parse(
      Source.fromFile(dataDir + "/conf/data/categories.json") mkString)
      .asInstanceOf[DBObject]

    Taxonomy.collection.remove(MongoDBObject())

    if (Taxonomy.collection.size == 0) {

      // Can't guarantee the order these will be read in so loop through all to 
      // create 
      // objectId's to later reference in parent and ancestors
      var idLookup = scala.collection.mutable.Map[Int, ObjectId]()
      docs.foreach(item => {
        val doc = item._2.asInstanceOf[DBObject]
        idLookup(doc.get("_id").asInstanceOf[Int]) = new ObjectId()
      })

      // Now actually perform the inserts
      docs.foreach(item => {
        val doc = item._2.asInstanceOf[DBObject]
        // Insert our id
        doc.put("_id", idLookup.get(doc.get("_id").asInstanceOf[Int]))

        var list: List[ObjectId] = List()
        if (doc.get("parent") != null) {
          // Update the parent
          doc.update("parent", idLookup.get(doc.get("parent").asInstanceOf[Int]))
          doc.get("ancestors").asInstanceOf[BasicDBList].toList.foreach(ref => {
            list ::= idLookup.get(ref.asInstanceOf[Int]).get.asInstanceOf[ObjectId]
          })
        }
        // Finally update the list of ancestors
        doc.update("ancestors", list)
        Taxonomy.collection.insert(doc)
      })

    }
  }
}