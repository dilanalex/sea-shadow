package controllers.backoffice

import play.api.mvc._
import com.novus.salat._
import com.mongodb.casbah.Imports._
import play.api.data._
import play.api.data.Forms._
import models._
import models.salatctx._
import play.api.mvc.Controller
import helpers._

/** provides controllers for shipping option handling
 *
 */
object ShippingOptions extends Controller {

  def tableRateForm(implicit request: RequestHeader): Form[ShippingTableRate] = Form(
    mapping(
      "tabRateId" -> nonEmptyText,
      "name" -> nonEmptyText,
      "description" -> optional(text),
      "taxableFlag" -> optional(boolean),
      "enableFrontFlag" -> optional(boolean),
      "enableAdminFlag" -> optional(boolean),
      "handlingFee" -> optional(text).verifying("Please enter a valid handling fee", handlingFee =>
        validateOptionalDigit(handlingFee)),
      "minWeight" -> optional(text).verifying("Please enter a valid minimum weight", minWeight =>
        validateOptionalDigit(minWeight)),
      "maxWeight" -> optional(text).verifying("Please enter a valid maximum weight", maxWeight =>
        validateOptionalDigit(maxWeight)),
      "obcAPI" -> optional(text),
      "rate" -> play.api.data.Forms.list(
        mapping(
          "country" -> nonEmptyText.verifying(
            "Please enter a valid Country",
            country => (!Country.findOne(MongoDBObject("code" -> country)).isEmpty || country.equals("*"))),
          "state" -> nonEmptyText.verifying(
            "Please enter valid State",
            state => (!State.findOne(MongoDBObject("code" -> state)).isEmpty || state.equals("*"))),
          "zipCode" -> optional(text),
          "city" -> optional(text),
          "minWeight" -> optional(text).verifying("Please enter valid Min Weight", minWeight =>
            validateOptionalDigit(minWeight)),
          "maxWeight" -> optional(text).verifying("Please enter valid Max Weight", maxWeight =>
            validateOptionalDigit(maxWeight)),
          "minVolumn" -> optional(text).verifying("Please enter valid Min Volumn", minVolumn =>
            validateOptionalDigit(minVolumn)),
          "maxVolumn" -> optional(text).verifying("Please enter valid Max Volumn", maxVolumn =>
            validateOptionalDigit(maxVolumn)),
          "minSubTotal" -> optional(text).verifying("Please enter valid Min SubTotal", minSubTotal =>
            validateOptionalDigit(minSubTotal)),
          "maxSubTotal" -> optional(text).verifying("Please enter valid Max SubTotal", maxSubTotal =>
            validateOptionalDigit(maxSubTotal)),
          "minItems" -> optional(text).verifying("Please enter valid Min Items", minItems =>
            validateOptionalDigit(minItems)),
          "maxItems" -> optional(text).verifying("Please enter valid Max Items", maxItems =>
            validateOptionalDigit(maxItems)),
          "rate" -> nonEmptyText.verifying("Please enter valid rate", rate =>
            rate.matches("""^[0-9]\d*(\.\d+)?$""")))((
            country,
            state,
            zipCode,
            city,
            minWeight,
            maxWeight,
            minVolumn,
            maxVolumn,
            minSubTotal,
            maxSubTotal,
            minItems,
            maxItems,
            rate) =>
            TableRate(country = country,
              state = state,
              zipCode = zipCode,
              city = city,
              minWeight = minWeight,
              maxWeight = maxWeight,
              minVolumn = minVolumn,
              maxVolumn = maxVolumn,
              minSubTotal = minSubTotal,
              maxSubTotal = maxSubTotal,
              minItems = minItems,
              maxItems = maxItems,
              rate = rate))(rate =>
            Some(rate.country,
              rate.state,
              rate.zipCode,
              rate.city,
              rate.minWeight,
              rate.maxWeight,
              rate.minVolumn,
              rate.maxVolumn,
              rate.minSubTotal,
              rate.maxSubTotal,
              rate.minItems,
              rate.maxItems,
              rate.rate))),
      "visibility" -> optional(play.api.data.Forms.list(text)),
      "countries" -> optional(play.api.data.Forms.list(text)))((
        tabRateId,
        name,
        description,
        taxableFlag,
        enableFrontFlag,
        enableAdminFlag,
        handlingFee,
        minWeight,
        maxWeight,
        obcAPI,
        rate,
        visibility,
        countries) =>
        ShippingTableRate(id = new ObjectId(tabRateId),
          name = name,
          description = description,
          taxableFlag = taxableFlag,
          enableFrontFlag = enableFrontFlag,
          enableAdminFlag = enableAdminFlag,
          handlingFee = handlingFee,
          minWeight = minWeight,
          maxWeight = maxWeight,
          obcAPI = obcAPI,
          rate = rate.toList,
          visibility = visibility,
          countries = countries))(shippingTableRate =>
        Some(shippingTableRate.id.toString,
          shippingTableRate.name,
          shippingTableRate.description,
          shippingTableRate.taxableFlag,
          shippingTableRate.enableFrontFlag,
          shippingTableRate.enableAdminFlag,
          shippingTableRate.handlingFee,
          shippingTableRate.minWeight,
          shippingTableRate.maxWeight,
          shippingTableRate.obcAPI,
          shippingTableRate.rate,
          shippingTableRate.visibility,
          shippingTableRate.countries)))

  def show(id: ObjectId) = Action { implicit request =>

    val customerGroups = models.CustomerGroup.findAll.toList
    var tableRates = getTableRate(id)

    val chkCustomerGroups = getChkCustomerGroups(tableRates)
    val countries = Country.find(MongoDBObject("enabled" -> ("true".toBoolean))).toList.sortBy(c => c.name)
    val chkCountries = getChkCountries(tableRates)

    Ok(views.html.backoffice.shippingOptions
      .show(tableRateForm.fill(tableRates), tableRates.rate,
        customerGroups, chkCustomerGroups, countries, chkCountries))
  }

  def list = Action { implicit request =>

    var shippingTableRates = ShippingTableRate.findAll.toList
    Ok(views.html.backoffice.shippingOptions.list(shippingTableRates))

  }

  def findShipTRlist(country: String, state: String) = Action { implicit request =>    
    //var shippingTableRates = ShippingTableRate.findAll.toList
    val c = Country.findOne(MongoDBObject("enabled" -> ("true".toBoolean), "code" -> country))
    var shippingTableRates = ShippingTableRate.find(MongoDBObject("enableFrontFlag" -> ("true".toBoolean)))
    var tableRatesJSON: String="{\"sTblRate\":["
    var foundCountry = false
    shippingTableRates.foreach(rateTbl => {
     var assCountriesList = rateTbl.countries.getOrElse(List()).asInstanceOf[DBObject]
     if(assCountriesList.size!=0){      
      assCountriesList.foreach(assignedCountry=>{         
        if(assignedCountry._2.toString().equalsIgnoreCase(c.get.id.toString())){
          foundCountry = true         
        }            
      })
     }else{      
        foundCountry = true
     }
      
      var flag = false     
      rateTbl.rate.sortWith((a,b)=>{a.country>b.country}).foreach(rates =>
        
        if(!flag && foundCountry){         
          if(country.equalsIgnoreCase(rates.country.trim()) && state.equalsIgnoreCase(rates.state.trim()) ){        	
        	tableRatesJSON = tableRatesJSON + 
        						"{\"name\":\""+rateTbl.name+"\"," + 
        						"\"description\":\""+rateTbl.description.getOrElse("")+"\"," + 
        						"\"rate\":\""+CurrencyConvertor.getSymbol(request)+" "+
        						    CurrencyConvertor.getRate(request,rateTbl.handlingFee.getOrElse("0").toDouble + 
        						    rates.rate.toDouble).toString+"\"},"        						 
       	  flag = true
          }
          if(rates.country.trim().equalsIgnoreCase(country.trim()) && rates.state.trim().equalsIgnoreCase("*") ){        	
        	tableRatesJSON = tableRatesJSON + 
        						"{\"name\":\""+rateTbl.name+"\"," + 
        						"\"description\":\""+rateTbl.description.getOrElse("")+"\"," + 
        						"\"rate\":\""+CurrencyConvertor.getSymbol(request)+" "+
        						    CurrencyConvertor.getRate(request,rateTbl.handlingFee.getOrElse("0").toDouble +
        						    rates.rate.toDouble).toString+"\"},"        						 
          flag = true
          } else if(rates.country.trim().equalsIgnoreCase("*") && rates.state.trim().equalsIgnoreCase("*") ){        	
        	tableRatesJSON = tableRatesJSON + 
        						"{\"name\":\""+rateTbl.name+"\"," + 
        						"\"description\":\""+rateTbl.description.getOrElse("")+"\"," + 
        						"\"rate\":\""+CurrencyConvertor.getSymbol(request)+" "+
        						    CurrencyConvertor.getRate(request,rateTbl.handlingFee.getOrElse("0").toDouble + 
        						    rates.rate.toDouble).toString+"\"},"        						 
          flag = true
          }
        }
        
      )
    })
    tableRatesJSON = tableRatesJSON + "]}"    
    Ok(tableRatesJSON)
  }
  
  def findShipTRlists(country: String, state: String): List[Tuple4[String, String, String, String]] =  {
    val c = Country.findOne(MongoDBObject("enabled" -> ("true".toBoolean), "code" -> country))
    var shippingTableRates = ShippingTableRate.find(MongoDBObject("enableFrontFlag" -> ("true".toBoolean)))
    var shipTRList: List[Tuple4[String, String, String, String]] = List.empty
    var foundCountry = false
    shippingTableRates.foreach(rateTbl => {
      var assCountriesList = rateTbl.countries.getOrElse(List()).asInstanceOf[DBObject]
      if(assCountriesList.size!=0){      
       assCountriesList.foreach(assignedCountry=>{         
        if(assignedCountry._2.toString().equalsIgnoreCase(c.get.id.toString())){
          foundCountry = true         
        }            
       })
      } else {      
        foundCountry = true
      }
      var flag = false     
      rateTbl.rate.sortWith((a,b)=>{a.country>b.country}).foreach(rates =>        
        if(!flag && foundCountry){         
          if(country.equalsIgnoreCase(rates.country.trim()) && state.equalsIgnoreCase(rates.state.trim()) ){
            shipTRList ++= List(new Tuple4(
                rateTbl.name, rateTbl.description.getOrElse(""),
                (rateTbl.handlingFee.getOrElse("0").toDouble + rates.rate.toDouble).toString,
                rateTbl.id.toString()
                ))        	        						 
       	  flag = true
          }
          if(rates.country.trim().equalsIgnoreCase(country.trim()) && rates.state.trim().equalsIgnoreCase("*") ){        	
        	shipTRList ++= List(new Tuple4(
                rateTbl.name, rateTbl.description.getOrElse(""),
                (rateTbl.handlingFee.getOrElse("0").toDouble + rates.rate.toDouble).toString,
                rateTbl.id.toString()
                ))
          flag = true
          } else if(rates.country.trim().equalsIgnoreCase("*") && rates.state.trim().equalsIgnoreCase("*") ){        	
        	shipTRList ++= List(new Tuple4(
                rateTbl.name, rateTbl.description.getOrElse(""),
                (rateTbl.handlingFee.getOrElse("0").toDouble + rates.rate.toDouble).toString,
                rateTbl.id.toString()
                ))
          flag = true
          }
        }        
      )
    })      
    shipTRList
  }

  def save = Action { implicit request =>
    val tabRateId = request.body.asFormUrlEncoded.get("tabRateId").head.toString()
    val customerGroups = CustomerGroup.findAll.toList
    val countries = Country.find(MongoDBObject("enabled" -> ("true".toBoolean))).toList.sortBy(c => c.name)

    var chkCg: List[String] = List.empty
    var chkCry: List[String] = List.empty

    for (a <- 0 to countries.size) {
      try {
        var cList = request.body.asFormUrlEncoded.get("country_" + a).head.toString()
        chkCry ++= List(cList.asInstanceOf[String])
      } catch {
        case t => // do nothing
      }
    }

    for (a <- 0 to customerGroups.size) {
      try {
        var cgList = request.body.asFormUrlEncoded.get("visibility_" + a).head.toString()
        chkCg ++= List(cgList.asInstanceOf[String])
      } catch {
        case t => // do nothing
      }
    }

    tableRateForm.bindFromRequest.fold(form => {

      var rates: List[TableRate] = List.empty
      var tableRates: ShippingTableRate = null
      var chkCountries: List[String] = List.empty
      var chkCustomerGroups: List[String] = List.empty

      if (ShippingTableRate.findOne(MongoDBObject("_id" ->
        new ObjectId(tabRateId))).isDefined) {
        tableRates = getTableRate(new ObjectId(tabRateId))
        rates = tableRates.rate
        chkCountries = getChkCountries(tableRates)
        chkCustomerGroups = getChkCustomerGroups(tableRates)
      }

      BadRequest(views.html.backoffice.shippingOptions
        .show(form, rates, customerGroups, chkCustomerGroups, countries, chkCountries))
        .flashing("errormessage" -> "Incomplete Form!!! Please check...")
    },

      tableRateForm => {
        if (ShippingTableRate.findOne(
          MongoDBObject("_id" -> new ObjectId(tabRateId))).isDefined) {
          ShippingTableRate.update(MongoDBObject("_id" -> new ObjectId(tabRateId)),
            grater[ShippingTableRate].asDBObject(tableRateForm), false, false)

          saveCustomerGroups(new ObjectId(tabRateId), chkCg)
          saveCountries(new ObjectId(tabRateId), chkCry)         

          Redirect(controllers.backoffice.routes.ShippingOptions.show(
            new ObjectId(tabRateId))).flashing("message" -> "Table Rates updated !!!")

        } else {
          ShippingTableRate.save(tableRateForm)
          saveCustomerGroups(new ObjectId(tabRateId), chkCg)
          saveCountries(new ObjectId(tabRateId), chkCry)

          Redirect(controllers.backoffice.routes.ShippingOptions.show(
            new ObjectId(tabRateId))).flashing("message" -> "Table Rates Saved !!!")
        }

      })
  }

  def delete(id: ObjectId) = Action { implicit request =>

    ShippingTableRate.remove(MongoDBObject("_id" -> id))
    Redirect(controllers.backoffice.routes.ShippingOptions.list())
      .flashing("message" -> "Table Rate deleted !!!")

  }

  def getTableRate(id: ObjectId): ShippingTableRate = {

    var obj: ShippingTableRate = ShippingTableRate.findOneById(id)
      .getOrElse(new ShippingTableRate)

    if (obj.visibility.isDefined) {
      var visibility: List[String] = List.empty
      obj.visibility.get.asInstanceOf[BasicDBList].toList.foreach(
        item => visibility ++= List(item.asInstanceOf[String]))
      obj.visibility = Some(visibility)
    }

    if (obj.countries.isDefined) {
      var countries: List[String] = List.empty
      obj.countries.get.asInstanceOf[BasicDBList].toList.foreach(
        item => countries ++= List(item.asInstanceOf[String]))
      obj.countries = Some(countries)
    }

    return obj
  }

  def validateOptionalDigit(variable: Option[String]): Boolean = {

    if (!variable.getOrElse("").equals("")) {
      variable.get.matches("""^[0-9]\d*(\.\d+)?$""")
    } else {
      true
    }
  }

  def getChkCustomerGroups(tableRates: ShippingTableRate): List[String] = {

    var chkCustomerGroups: List[String] = List.empty
    chkCustomerGroups = tableRates.visibility.getOrElse(List.empty)
    chkCustomerGroups

  }

  def getChkCountries(tableRates: ShippingTableRate): List[String] = {

    var chkCountries: List[String] = List.empty
    chkCountries = tableRates.countries.getOrElse(List.empty)
    chkCountries

  }

  def saveCustomerGroups(id: ObjectId, chkCg: List[String]) = {

    val tableRates = getTableRate(id)
    tableRates.addCustomerGroups(chkCg)
    ShippingTableRate.save(tableRates)

  }

  def saveCountries(id: ObjectId, chkCry: List[String]) = {

    val tableRates = getTableRate(id)
    tableRates.addCountries(chkCry)
    ShippingTableRate.save(tableRates)

  }
  
  
}
