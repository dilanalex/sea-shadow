/*import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import models.Product$

class ProductSpec extends Specification {
  
  "Products" should {
    "list products on /products" in {
      running(FakeApplication(additionalConfiguration = Map("mongodb.default.db" -> "obc-test"))) {
	      val Some(result) = routeAndCall(FakeRequest(GET, "/products"))
	      status(result) must equalTo(OK)
	      contentType(result) must beSome("text/html") 
	      contentAsString(result) must (contain("""These aren't the products 
	          you're looking for!""") or
	          (contain("""<div class="span3 product">""") and
	          find("a href=\"/products/.*\"")))
      }
    }
    
    "display a product when the objectid is known and return a 404 when the product does not exist" in {
      running(FakeApplication(additionalConfiguration = Map("mongodb.default.db" -> "obc-test"))) {
        // First create a product
        val product = models.Product(
          title = "Test Product",
          sku = "000",
          price = 10.99,
          description = "a test product",
          image = "hoodie"
        )
        models.Product.save(product)
        
        val Some(result) = routeAndCall(FakeRequest(GET, "/products/" + product.id))
        status(result) must equalTo(OK)
        contentType(result) must beSome("text/html") 
	    contentAsString(result) must contain("<h1>" + product.title + "</h1>")
        contentAsString(result) must contain("<h2>£" + product.price + "</h2>")
        contentAsString(result) must contain("<p>" + product.description + "</p>")
        contentAsString(result) must contain("""<img src="/assets/temp-images/products/hoodie-lg.jpg" />""")
        
        
        // Delete the product and re-make request
        models.Product.remove(product)
        
        val Some(result2) = routeAndCall(FakeRequest(GET, "/products/" + product.id))
        status(result2) must equalTo(404)
        
      }
    }
  }
  
}*/