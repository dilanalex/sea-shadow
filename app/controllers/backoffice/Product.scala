package controllers.backoffice

import scala.math.BigDecimal.double2bigDecimal
import com.mongodb.casbah.Imports._
import com.novus.salat.grater
import models._
import models.salatctx._
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.RequestHeader
import utils.Files
import utils.ImageProcess
import play.api.mvc.AnyContent
import java.io.File
import play.Play
import java.util.regex.Pattern
import be.objectify.deadbolt.actions.Restrict
import be.objectify.deadbolt.scalabolt.Scalabolt
import security.MyScalaboltHandler
import play.api.data._
import play.api.data.Forms._
import helpers.Page
import scala.collection.mutable.ListBuffer

object Product extends Controller with Scalabolt with Secured {
  val imagePath = "/public/images/products/"

  def productForm(implicit request: RequestHeader) : Form[Product] = Form(
    mapping(
      "productid" -> nonEmptyText,
      "sku" -> nonEmptyText,
      "title" -> nonEmptyText,
      "price" -> nonEmptyText.verifying("Please enter price", price => price.matches("""^[0-9]\d*(\.\d+)?$""")),
      "description" -> nonEmptyText,
      "image" -> play.api.data.Forms.list( nonEmptyText ),
      "taxClassId" -> optional(text),
      "metaDescription" -> optional(text),
      "metaKeywords" -> optional(text),
      "relatedProducts" -> play.api.data.Forms.list(text)
      )
      ((productid, sku, title, price, description, image, taxClassId, metaDescription, metaKeywords, relatedProducts) => 
        models.Product(id = new ObjectId(productid),
        sku = sku, title = title, price = price.toDouble, description = description, 
        image = image.toList, taxClassId = taxClassId, metaDescription = metaDescription,
        metaKeywords = metaKeywords, relatedProducts = relatedProducts.toList))
      (product => Some(product.id.toString(), product.sku, product.title, 
          product.price.toString(), product.description, product.image, 
          product.taxClassId, product.metaDescription, product.metaKeywords, product.relatedProducts))
  )

  def edit(id: ObjectId) = Action { implicit request =>   
    val relatedProducts = new ListBuffer[models.Product]
    for ( relProd <- getProduct(id).relatedProducts ) {
      relatedProducts += models.Product.findOne(MongoDBObject("_id" -> new ObjectId(relProd))).getOrElse(new models.Product)
    }
    Ok(views.html.backoffice.product.view(productForm.fill(getProduct(id)), getProduct(id).image, relatedProducts.toList))    
  }

  def delete(id: ObjectId) = Action { implicit request =>
    models.Product.remove(MongoDBObject("_id" -> id))
    Redirect(controllers.backoffice.routes.Product.list()).flashing("message" -> "Product deleted !!!")
  }
  
  def deleteImage() = Action { implicit request =>    
    val product = models.Product.findOneById(parseObjectId(request.body, "productid")).get
    val imgName = request.body.asFormUrlEncoded.get.get("imgName").get.head    
    product.image = product.image.filterNot(im => im.equalsIgnoreCase(imgName))
    deletePhysicalImage(imgName)
    if(product.image.size == 0)
    product.image = List("no_Image.jpg") 
    models.Product.update(MongoDBObject("_id" -> product.id), grater[Product].asDBObject(product), false, false)    
    Redirect(controllers.backoffice.routes.Product.edit(product.id)).flashing("message" -> "Successfully deleted the image")
  }
  
  def parseObjectId(body: AnyContent, field: String): ObjectId = {
    new ObjectId(body.asFormUrlEncoded.get.get(field).get.head)
  }
  
  def replaceImage = Action(parse.multipartFormData) { implicit request =>   
    val prodId = request.body.asFormUrlEncoded.get("productid").get.head
    val index = request.body.asFormUrlEncoded.get("imgId").get.head
    val imgName = request.body.asFormUrlEncoded.get("imgName").get.head   
    val filename = prodId + "_" + System.currentTimeMillis + ".jpg"
      if (ImageProcess.uploadImage(request, imagePath, filename, "picture"+index)) {        
         var product = getProduct(new ObjectId(prodId))
         product.image = product.image.splitAt((index.toInt))._1 ++ List(filename) ++ product.image.splitAt((index.toInt+1))._2
         deletePhysicalImage(imgName)
         models.Product.update(MongoDBObject("_id" -> new ObjectId(prodId)), grater[Product].asDBObject(product), false, false) 
         Redirect(controllers.backoffice.routes.Product.edit(new ObjectId(prodId))).flashing("message" -> "Successfully replaced the image")
      } else {
       Redirect(controllers.backoffice.routes.Product.edit(new ObjectId(prodId))).flashing("errormessage" -> "Select an image to replace")
      }    
    
  }

  def save = Action(parse.multipartFormData) { implicit request =>
    var prodId = request.body.asFormUrlEncoded.get("productid").get.head.toString()
    var img = List("no_Image.jpg")
    if(models.Product.findOne(MongoDBObject("_id" -> new ObjectId(prodId))).isDefined){ 
      img = getProduct(new ObjectId(prodId)).image
    }
    
    productForm.bindFromRequest.fold(
      form => {BadRequest(views.html.backoffice.product.view(form, img))},
      prodForm => {              
        var message = "Product Updated !!!"         
         
        request.body.files.map { picture =>
          var filename = picture.filename        
          if (!filename.isEmpty) {
            filename = prodId + "_" + System.currentTimeMillis + ".jpg"          
            picture.ref.moveTo(new File(Play.application.path + imagePath + filename), true)          
          }
         
          if(!(prodForm.image.size == 1 && prodForm.image.head == "no_Image.jpg")){
            prodForm.image = prodForm.image ::: List(filename)  
          } else {          
            prodForm.image = List(filename)
          }         
          message = "Image uploaded successfully !!!"        
        }
        
        if(models.Product.findOne(MongoDBObject("_id" -> new ObjectId(prodId))).isDefined){                   
          models.Product.update(MongoDBObject("_id" -> new ObjectId(prodId)), grater[Product].asDBObject(prodForm), false, false)          
          Redirect(controllers.backoffice.routes.Product.edit(new ObjectId(prodId))).flashing("message" -> message)
        } else {          
          models.Product.save(prodForm)
          Redirect(controllers.backoffice.routes.Product.edit(new ObjectId(prodId))).flashing("message" -> "Product Saved !!!")
        }
      }
    )   
  }
  
  def deletePhysicalImage(img : String) = {
   if (!(img.equals("") || img.equals("no_Image.jpg"))) {                 
     Files.deleteFile(img, imagePath)
     Files.deleteFilePattern(new File(Play.application.path + imagePath + "resized/"), Pattern.compile(".*"+img))
   }
  }

  //Display product list to be edited
  /*def list = SBRestrict(Array("foo"), new MyScalaboltHandler){    
    Action { implicit request =>    
      val products = models.Product.findAll.toList.grouped(1)
      Ok(views.html.backoffice.product.list(products))
    }
  }*/

  def list = IsAuthenticated { username =>
    implicit request =>
      val products = models.Product.findAll.toList.grouped(1)
      Admin.findOne(MongoDBObject("username" -> username)).map 
      { admin =>
        var usrToSend: Admin =
          new Admin(new ObjectId, admin.username, Option(""), admin.password, admin.adminType)
        Ok(views.html.backoffice.product.list(products))
      }.getOrElse(Forbidden)
  	}

  def getProduct(id: ObjectId): models.Product = {
    models.Product.findOne(MongoDBObject("_id" -> id)).getOrElse(new models.Product)
  }
  
  def listproducts(page: Int, orderBy: Int, filter: String = "%") = Action {
    implicit request =>
      val offest = 10 * page
      var pattern = ".*(?i)" + filter + ".*";
      val descendents = List("p01", "p08")
      var products = models.Product.find({MongoDBObject("title" -> pattern.r)})
        .skip(offest).limit(10).toList
      var records =
        models.Product.find({MongoDBObject("title" -> pattern.r)}).size
      Ok(views.html.backoffice.product.productpopup(Page(products, page, offest,
        records), orderBy, filter))
  }
  
  def listRelatedProducts(skuList: String, startIndex : Int) = Action { implicit request =>
    var prodArr = skuList.split(",")
    var buf = ""
    var x = startIndex  
    for (prod <- prodArr){
      var productFromDB = models.Product.findOne( MongoDBObject( "sku" -> 
              prod ) ).getOrElse( null )
      buf += "<tr id='"+productFromDB.sku+"'><td width='50%'>"+productFromDB.title+"</td>"
      buf += "<td width='22%'>"+productFromDB.sku+"</td>"
      buf += "<td>"+productFromDB.price+"</td>"
      buf += """<td><input type='button' class='btn btn-danger' value='Remove' 
        onclick='removethis(""""+productFromDB.sku+"""")' /></td></tr>"""
      buf += "<input type='hidden' value='"+productFromDB.id+"' " +
          "name='relatedProducts["+x+"]' id='relatedProducts["+x+"]' >"
      x=x+1
    }
    Ok(buf)
  }

}