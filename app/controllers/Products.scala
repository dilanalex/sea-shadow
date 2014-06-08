package controllers

import play.api._
import play.api.mvc._
import se.radley.plugin.salat._
import com.mongodb.casbah.Imports._
import com.novus.salat._
import models.salatctx._
import scala.collection.mutable.MutableList
import models._
import helpers.Taxonomies
import com.traackr.scalastic.elasticsearch.Indexer
import org.elasticsearch.index.query.QueryBuilders
import scala.collection.JavaConversions._
import models.Taxonomy
import play.api.i18n.Messages
import models.Reviews
import securesocial.core.SecureSocial
import scala.collection.mutable.ListBuffer

object Products extends Controller {  

  def index = Action { implicit request =>
    val products = Product.findAll.toList
    Ok( views.html.product.index(Application.getLoggedInUser(request), products))
  }

  def search( path: String ) = Action { implicit request =>
    val taxonomy = Taxonomies.taxonomy( path )
    val descendents = taxonomy.id ::
      Taxonomies.descendents( taxonomy ).map( item => item.id ).toList
    val products = Product.find( "categories" $in descendents ).toList

    Ok( views.html.product.index(Application.getLoggedInUser(request), products, taxonomy ) )
  }

  def show( id: ObjectId ) = Action { implicit request =>
    models.Product.findOneById( id ).map( product => {
      var taxonomy: Taxonomy = null
      if ( product.categories.size > 0 ) {
        taxonomy = Taxonomies.taxonomy( product.categories.head )
      }
      val reviews = models.Reviews.find(MongoDBObject("product_id" -> new ObjectId(id.toString()))).toList
      val relatedProducts = new ListBuffer[models.Product]
      for ( relProd <- product.relatedProducts ) {
        relatedProducts += models.Product.findOne(MongoDBObject("_id" -> new ObjectId(relProd))).get
      }
      Ok( views.html.product.show(Application.getLoggedInUser(request), product, taxonomy, relatedProducts.toList, reviews ) )
    } ).getOrElse( NotFound )
  }

  def searchProduct( searchtext: String ) = Action { implicit request =>

    if ( !searchtext.isEmpty() ) {
      try {
        val indexer = Indexer.transport( settings =
          Map( ( "cluster.name", Play.current.configuration.getString(
            "elasticsearch.cluster.name" ).getOrElse( "elasticsearch" ) ) ), host =
          Play.current.configuration.getString(
            "elasticsearch.mongodb.host" ).getOrElse( "localhost" ) )
        val response = indexer.search( indices =
          List( Play.current.configuration.getString(
            "elasticsearch.mongodb.productindex.name" ).getOrElse(
              "productindex" ) ), query = QueryBuilders.
          queryString( "title:" + searchtext.trim()
            + "^" + Play.current.configuration.getString(
              "elasticsearch.mongodb.productindex.titleboost" ).getOrElse(
                "10" ) + " description:" + searchtext.trim() + "^" + Play.current.configuration.getString(
                  "elasticsearch.mongodb.productindex.descriptionboost" ).getOrElse(
                    "2" ) + " " + searchtext.trim() ) )

        var productArray: List[models.Product] = List.empty
        if ( response.getHits().getTotalHits() != 0 ) {

          for ( tes <- response.getHits().getHits() ) {

            productArray ++= List(
              new models.Product(
                id = new ObjectId( tes.getSource().get( "_id" ).toString() ),
                sku = tes.getSource().get( "sku" ).toString(),
                title = tes.getSource().get( "title" ).toString(),
                price = tes.getSource().get( "price" ).toString().toDouble,
                description = tes.getSource().get( "description" ).toString(),
                image = tes.getSource().get( "image" )
                  .asInstanceOf[java.util.ArrayList[String]].toList,
                categories = tes.getSource().get( "categories" )
                  .asInstanceOf[java.util.ArrayList[ObjectId]].toSet,
                relatedProducts = tes.getSource().get( "relatedProducts" )
                  .asInstanceOf[java.util.ArrayList[String]].toList ) )

          }
          Ok( views.html.product.index(Application.getLoggedInUser(request), productArray.toList ) )
        }
        else {
          Redirect( routes.Products.index() )
            .flashing( "message" -> Messages( "home.big.label" ) )
        }

      }
      catch {
        case exception: org.elasticsearch.client.transport.NoNodeAvailableException => {
          println( "ES down" )
          Redirect( routes.Products.index() )
            .flashing( "message" -> Messages( "common.no.search.provider" ) )

        }
        case allotherExceptions =>
          println( allotherExceptions.getMessage() )
          Redirect( routes.Products.index() )
            .flashing( "message" -> Messages( "common.no.search.provider" ) )
      }
    }
    else {
      Redirect( routes.Products.index() )
        .flashing( "message" -> Messages( "product.empty.search" ) )

    }

  }

}