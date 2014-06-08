package controllers.backoffice

import play.api.mvc._
import play.api.data.Forms._
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import java.io.InputStreamReader
import java.io.FileInputStream
import java.io.BufferedReader
import java.io.File
import play.Play
import com.mongodb.casbah.Imports._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.Set
import models._
import com.novus.salat.global._
import com.novus.salat._
import com.mongodb.casbah.Imports.MongoDBObject
import com.mongodb.casbah.Imports.ObjectId
import com.novus.salat.grater
import models.salatctx._



object ProductUpload extends Controller{

  val filePath = "\\conf\\data\\"
  val imagePath = "\\public\\images\\products\\"
 
  /**
   * Handle default path requests, redirect to computers list
   */  
  def index = Action {  implicit request =>
    Ok(views.html.backoffice.productupload.index(""))
  }
  
  /**
   * Param : form with csv file
   * Upload products and categories with related products
   */
  def upload = Action(parse.multipartFormData) { request =>
	  request.body.file("fileupload").map { file =>
	    val filename = file.filename
	    val contentType = file.contentType
	    print(Play.application.path + filePath + filename)
	    file.ref.moveTo(new File(Play.application.path + filePath + filename), true)
	    uploadProductCategories(filename)
	    uploadProducts(filename)
	    uploadRelatedProducts(filename)
	    Redirect(controllers.backoffice.routes.ProductUpload.index).
	    flashing("message" -> "Products uploaded successfully !!!")
	  }.getOrElse {
	    Redirect(controllers.backoffice.routes.ProductUpload.index).
	    flashing("errormessage" -> "File Missing")
	  }
	}
  
  /**
   * Param : form with image list
   * Upload images to the application : multiple images upload
   */
  def uploadimage = Action(parse.multipartFormData) { request =>
	  request.body.files.map { file =>
	    var filename = file.filename        
      if (!filename.isEmpty) {
        file.ref.moveTo(new File(Play.application.path + imagePath + filename), true)          
      }
	  }
	  Redirect(controllers.backoffice.routes.ProductUpload.index).
	    flashing("message" -> "Image uploaded successfully !!!")
	}
  
  /**
   * Param : file name to be uploaded
   * Product categories(Taxonomies) upload function
   */
  def uploadProductCategories(fileName : String) = {
    val reader = new CSVReader( new FileReader( Play.application.path + 
        filePath + fileName ) )
    var taxonomySet: Set[String] = Set[String]()
    var pathSet: Set[String] = Set[String]()
    for ( row <- reader.readAll ) {
      pathSet += row( 4 ).trim()
    }
    for ( path <- pathSet ) {
      var pathsplit = path.split( "/" )
      for ( i <- 0 to ( pathsplit.size - 1 ) ) {
        var name = pathsplit( i ).trim()
        if ( !name.equalsIgnoreCase( "" ) && !name.equalsIgnoreCase( "_category" ) ) {
          if ( i == 0 ) {
            insertTaxonomy( name, name, null, null )
          }
          else {
            var tempPath = ""
            var parent: ObjectId = null
            var ancestorslist: List[ObjectId] = List()
            for ( j <- 0 to i ) {
              tempPath = tempPath + pathsplit( j ).trim()
              var taxonomyFromDB = models.Taxonomy.findOne( MongoDBObject( "path" -> 
              tempPath.trim() ) ).getOrElse( null )
              tempPath = tempPath + "/"
              if ( taxonomyFromDB != null ) {
                if ( j == i - 1 ) {
                  parent = taxonomyFromDB.id
                }
                ancestorslist ++= List( taxonomyFromDB.id )
              }
            }
            insertTaxonomy( tempPath.substring( 0, tempPath.lastIndexOf( "/" ) ),
                name, parent, ancestorslist )
          }
        }
      }
    }
  }
  
  /**
   * Param : filen name to be uploaded
   * Upload products separately
   */
  def uploadProducts(fileName : String) = {
	    //Inserting products to the DB using specified CSV
	    val productReader = new CSVReader( new FileReader( Play.application.path +
	        filePath + fileName ) )
	    var x = 0;
	    for ( row <- productReader.readAll ) {
	      var catList: Set[ObjectId] = Set.empty
	      if(row(0) != "" || row(4) != ""){
	      	var taxonomyObj = models.Taxonomy.findOne( MongoDBObject( "path" -> row(4)
	      	    .trim().replaceAll(" / ","/") ) ).getOrElse( null )
	      	if ( taxonomyObj != null ) {
	      		catList = Set(taxonomyObj.id)
	      	}
	      }
	      if(row(0) != "" && x != 0){
	        var imgList: List[String] = Nil
	        if(row(30).lastIndexOf('/') == -1){
	          imgList = List("no_Image.jpg")
	        }else{
	          imgList = List(row(30).substring((row(30).lastIndexOf('/')+1)))
	        }
		      models.Product.save(new models.Product(
	          id = new ObjectId(),
	          title = row(46),
	          description = row(20),
	          sku = row(0),
	          price = if ( !row( 51 ).trim().equalsIgnoreCase( "" ) ) 
	            BigDecimal( row( 51 ).trim().toDouble ) else BigDecimal( 0.0 ),
	          image = imgList,
	          categories = catList,
	          relatedProducts = List.empty
		     ))
	     }
	     x=x+1
	    }
  }
  
  /**
   * Param : file name to be uploaded
   * Related products are uploaded separately
   */
  def uploadRelatedProducts(fileName : String) = {
  	//Inserting related products to the products entered above
    val reader2 = new CSVReader( new FileReader( Play.application.path +
        filePath + fileName ) )
    var relatedProductList: ListBuffer[String] = ListBuffer[String]()
    var sku = ""
    var i: Int = 0;
    for ( row <- reader2.readAll ) {
      if ( i != 0 ) {
        if ( !row( 0 ).trim().equalsIgnoreCase( "" ) ) {
          if ( !sku.equalsIgnoreCase( "" ) ) {
            val product = models.Product.findOne(MongoDBObject("sku" -> sku)).get
            product.relatedProducts = relatedProductList.toList
            models.Product.update(MongoDBObject("sku" -> sku),
                 grater[Product].asDBObject(product), false, false)
            sku = ""
            relatedProductList = ListBuffer[String]()
          }
          sku = row( 0 ).trim()
          if ( !row( 98 ).trim().equalsIgnoreCase( "" ) ) {
            var productObj = models.Product.findOne( MongoDBObject( "sku" -> row( 98 )
                .trim() ) ).getOrElse( null )
            relatedProductList += productObj.id.toString
          }
        }
        else {
          if ( !row( 98 ).trim().equalsIgnoreCase( "" ) ) {
            var productObj = models.Product.findOne( MongoDBObject( "sku" -> row( 98 )
                .trim() ) ).getOrElse( null )
            relatedProductList += productObj.id.toString
          }
        }
      }
      i += 1;
    }
  }
  
  /**
   * To insert categories(Taxonomies)
   */
  def insertTaxonomy( path: String, name: String, parent: ObjectId, ancestorslist: List[ObjectId] ) {
    var fromDB = models.Taxonomy.findOne( MongoDBObject( "path" -> path.trim() ) ).getOrElse( null )
    if ( fromDB == null ) {
      var tempTaxonomy = new Taxonomy( id = new ObjectId(), name = name, path = path,
        parent = Some( parent ), ancestors = ancestorslist )
      models.Taxonomy.save( tempTaxonomy )

    }
  }
  
}