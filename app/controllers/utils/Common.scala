package controllers.utils

import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.Enumerator
import utils.ImageProcess
import java.net.URLConnection
import models._
import models.salatctx._
import com.mongodb.casbah.Imports._
import java.io.OutputStreamWriter
import java.net.{URLConnection, URL}
import java.net.URLEncoder.encode
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.InputStream
import securesocial.core.SecureSocial
import play.api.i18n.Messages


object Common extends Controller {

  /*
  * serves an image upon a request 
  */
  def getImage(imageName: String, width: Int, height: Int) =
    Action { implicit request =>
      val imageFile = ImageProcess.pictureResize(imageName, width, height)

      val fileContent: Enumerator[Array[Byte]] = Enumerator
        .fromFile(imageFile)

      SimpleResult(
        header = ResponseHeader(200, Map(
          CONTENT_LENGTH -> imageFile.length().toString,
          CONTENT_TYPE -> URLConnection.getFileNameMap()
            .getContentTypeFor(imageName))),
        body = fileContent)

    }
  
   /*
   * update the currency rates from www.webservicex.net 
   */
   def updateCurrencyRates = Action { implicit request =>
     val defaultCurrency = Currency.findOne(MongoDBObject("enabled" -> ("true".toBoolean), 
    		 											  "baseCurrency" -> ("true".toBoolean)))
     //println("defaultCurrency="+defaultCurrency.get.iso)
     val curriencies = Currency.find(MongoDBObject("enabled" -> ("true".toBoolean))).toList.sortBy(c => c.name)
     //println("curriencies1="+curriencies)     
     var msgkey = "message"
     var msg = Messages( "backoffice.currency.sync.success" )
     var reqResCount = 0
     
     curriencies.foreach( c => {
       try {
         //println("curriencies2="+c.iso) 
         //println("curriencies For="+c.iso)
         reqResCount = reqResCount + 1         
         var url = "http://www.webservicex.net/CurrencyConvertor.asmx/ConversionRate?FromCurrency="+
    		   		defaultCurrency.get.iso+
    		   		"&ToCurrency="+c.iso       
         
         var result: Promise[play.api.libs.ws.Response] = { WS.url(url).get() }
         var rate: BigDecimal = 0.0000  
         result.map{ res => {
           //println(res.status)
           if(res.status == 200){
             //println("Rate= "+res.xml.child.head)
             rate = res.xml.child.head.toString.toDouble        
             c.rate = rate
             //println("curriencies3="+c.iso)
             Currency.save(c)  
           } else {
              msgkey = "errormessage"
              msg="Currency rates were not synchronized successfully !!!"       
           }        
         }
         reqResCount = reqResCount - 1
         }       
        } catch {
            case t => Redirect(controllers.backoffice.routes.Currencies.list).flashing(
            "errormessage" -> "Currency rates were not synchronized successfully !!!")
        }      
     })
     
     var responseCheck = true
     synchronized {
       while(responseCheck){
         //print(reqResCount)
         if(reqResCount == 0){
           responseCheck = false
           Redirect(controllers.backoffice.routes.Currencies.list).flashing(msgkey -> msg)    
         }         
       }
     }      
      Redirect(controllers.backoffice.routes.Currencies.list).flashing(msgkey -> msg)        
    }

def Post(url: String, data: Map[String, String]) = {  
    val u = new URL(url)
    val conn = u.openConnection
    val HttpRequestTimeout = 15000
    val userAgent = "Mozilla/5.0"
    
    conn.setRequestProperty("User-Agent", userAgent)
    conn.setConnectTimeout(HttpRequestTimeout)
    conn.setDoOutput(true)
    conn.connect

    val wr = new OutputStreamWriter(conn.getOutputStream())
    wr.write(encodePostData(data))
    wr.flush
    wr.close

    conn.getInputStream
    
  }

  def readInputStream(data : InputStream)  :String = {
    var is : InputStreamReader = new InputStreamReader(data)
    var sb : StringBuilder = new StringBuilder()
    var br : BufferedReader = new BufferedReader(is)
    var read : String  = br.readLine()

    while(read != null) {      
      sb.append(read)
      read = br.readLine()
    }

  sb.toString()
  }

  private def encodePostData(data: Map[String, String]) = {
    val encoding = "ASCII"
    (for ((name, value) <- data) yield encode(name, encoding) + "=" + encode(value, encoding)).mkString("&")
  }

}