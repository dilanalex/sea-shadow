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
import java.util.{Date}
import scala.collection.mutable.ListBuffer

/** provides controllers for PriceRules handling
 *
 */
object PriceRules extends Controller {

  def catalogPriceRulesForm(implicit request: RequestHeader): Form[CatalogPriceRules] = Form(
    mapping(
      "catalogPriceRuleId" -> nonEmptyText,
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "active" -> optional(boolean),
      "dateStart" -> optional(date("dd-MM-yyyy")),
      "dateEnd" -> optional(date("dd-MM-yyyy")),
      "customerGroups" -> optional(list(text)),
      "conditions" -> list(
        mapping(
          "conditionId" -> nonEmptyText,
          "attribute" -> nonEmptyText,
          "operator" -> optional(text),
          "cvalue" -> optional(text),
          "products" -> list(tuple("id"-> text, "name"->text))
          )
          ((conditionId, attribute, operator, cvalue, products) =>
            RuleCondition(id = new ObjectId(conditionId), attribute = attribute, operator = operator,
            cvalue = cvalue, products = products.toList
          ))
          (ruleCondition => Some(ruleCondition.id.toString, ruleCondition.attribute, ruleCondition.operator,
            ruleCondition.cvalue, ruleCondition.products        
          ))
        ),
        "terminatingRule" -> optional(boolean),
        "action" -> nonEmptyText,
        "discountAmount" -> nonEmptyText.verifying("Please enter discount amount", 
            discountAmount => discountAmount.matches("""^[0-9]\d*(\.\d+)?$"""))
    )
    ((catalogPriceRuleId, name, description, active, dateStart, dateEnd,
        customerGroups, conditions, terminatingRule, action, discountAmount) =>
        CatalogPriceRules(id = new ObjectId(catalogPriceRuleId), name = name, description = description,
        active = active, dateStart = dateStart, dateEnd = dateEnd, customerGroups = customerGroups, 
        conditions = conditions.toList, terminatingRule = terminatingRule, action = action, 
        discountAmount = discountAmount.toDouble
    ))
    (catalogPriceRules => Some(catalogPriceRules.id.toString, catalogPriceRules.name, 
        catalogPriceRules.description, catalogPriceRules.active, 
        catalogPriceRules.dateStart, catalogPriceRules.dateEnd, catalogPriceRules.customerGroups,
        catalogPriceRules.conditions, catalogPriceRules.terminatingRule,
        catalogPriceRules.action, catalogPriceRules.discountAmount.toString()
    ))
  )
  
  def show(id: ObjectId, tab: Int) = Action { implicit request =>     
    val catalogPriceRules = getCatalogPriceRules(id)    
    val chkCustomerGroups = getChkCustomerGroups(catalogPriceRules)
    val conditionRuleList = catalogPriceRules.conditions
    Ok(views.html.backoffice.catalogPriceRules.show(catalogPriceRulesForm.fill(catalogPriceRules), 
        chkCustomerGroups, tab, conditionRuleList))
  }
  
  def save = Action { implicit request =>
    val catalogPriceRuleId = request.body.asFormUrlEncoded.get("catalogPriceRuleId").head.toString()
    val tab = request.body.asFormUrlEncoded.get("tab").head.toInt
    //println("tab="+tab)
    
    var chkCg: List[String] = List.empty
    for (a <- 0 to CustomerGroup.customerGroups.size) {
      try {
        var cgList = request.body.asFormUrlEncoded.get("cg_" + a).head.toString()
        chkCg ++= List(cgList.asInstanceOf[String])
      } catch {
        case t => // do nothing
      }
    }
    catalogPriceRulesForm.bindFromRequest.fold(form => {
      var chkCustomerGroups: List[String] = List.empty
      var conditionRuleList: List[RuleCondition] = List.empty
      if (CatalogPriceRules.findOne(MongoDBObject("_id" ->
        new ObjectId(catalogPriceRuleId))).isDefined) {
        val catalogPriceRules = getCatalogPriceRules(new ObjectId(catalogPriceRuleId))        
        chkCustomerGroups = getChkCustomerGroups(catalogPriceRules)
        conditionRuleList = catalogPriceRules.conditions
      }
      BadRequest(views.html.backoffice.catalogPriceRules.show(form, 
          chkCustomerGroups, tab, conditionRuleList)).flashing(
          "errormessage" -> "Incomplete Form!!! Please check...")
    },
    catalogPriceRulesForm => {
      //println("catalogPriceRulesForm="+catalogPriceRulesForm)      
    	if (CatalogPriceRules.findOne(
          MongoDBObject("_id" -> new ObjectId(catalogPriceRuleId))).isDefined) {
          CatalogPriceRules.update(MongoDBObject("_id" -> new ObjectId(catalogPriceRuleId)),
            grater[CatalogPriceRules].asDBObject(catalogPriceRulesForm), false, false)
            saveCustomerGroups(new ObjectId(catalogPriceRuleId), chkCg)

          Redirect(controllers.backoffice.routes.PriceRules.show(
            new ObjectId(catalogPriceRuleId), tab)).flashing("message" -> "Catalog Price Rules updated !!!")
        } else {
          val cpr = CatalogPriceRules.findAll
          if(!cpr.isEmpty){
            val maxPriority = cpr.toList.sortBy(c => c.priority).last.priority
            //println("maxPriority="+maxPriority)         
            catalogPriceRulesForm.priority = maxPriority + 1
          }
          CatalogPriceRules.save(catalogPriceRulesForm)
          saveCustomerGroups(new ObjectId(catalogPriceRuleId), chkCg)
          Redirect(controllers.backoffice.routes.PriceRules.show(
            new ObjectId(catalogPriceRuleId), tab)).flashing("message" -> "Catalog Price Rules Saved !!!")
        }
    })
  }

  def getCatalogPriceRules(id: ObjectId): CatalogPriceRules = {
    var obj = CatalogPriceRules.findOneById(id).getOrElse(new CatalogPriceRules)
    
    if (obj.customerGroups.isDefined) {
      var customerGroups: List[String] = List.empty
      obj.customerGroups.get.asInstanceOf[BasicDBList].toList.foreach(
        cg => customerGroups ++= List(cg.asInstanceOf[String]))
      obj.customerGroups = Some(customerGroups)
    }
    
    if (!obj.conditions.isEmpty) {
      var rc: List[RuleCondition] = List.empty      
      obj.conditions.foreach( con => {
          var ruleConditions = con.asInstanceOf[RuleCondition]
          var productsList: List[Tuple2[String,String]]=List.empty
           ruleConditions.products.asInstanceOf[List[DBObject]].foreach( pro => {            
             var keysetItr = pro.keySet().iterator()
             while(keysetItr.hasNext()){               
            	 var kenext = keysetItr.next().toString()
                productsList ++= List((kenext, pro.get( kenext).toString()))
             }           
           })
          ruleConditions.products = productsList         
          rc ++= List(ruleConditions)
      })
      obj.conditions = rc
    }   
       
    return obj
  }

  def getChkCustomerGroups(catalogPriceRules: CatalogPriceRules): List[String] = {
    var chkCustomerGroups: List[String] = List.empty
    chkCustomerGroups = catalogPriceRules.customerGroups.getOrElse(List.empty)
    chkCustomerGroups
  }
  
  def saveCustomerGroups(id: ObjectId, chkCg: List[String]) = {
    val catalogPriceRules = getCatalogPriceRules(id)
    catalogPriceRules.addCustomerGroups(chkCg)
    CatalogPriceRules.save(catalogPriceRules)
  }
  
  def listproducts(page: Int, orderBy: Int, filter: String = "%", opt: String, 
      op: String, id1: String, id2: String, tr: String) = Action { implicit request =>
      var ruleCondition = RuleCondition(attribute="", products=Nil, operator=Some(""), cvalue=Some(""))
      if(!id1.equals("")){
        val catalogPriceRules = getCatalogPriceRules(new ObjectId(id1))       
        ruleCondition = catalogPriceRules.conditions.find(co=> co.id==new ObjectId(id2)).get         
      }      
      val offest = 5 * page      
      var pattern = ".*(?i)" + filter + ".*";
      //print("offest = "+offest)
      var mainTaxonomies = Taxonomy.find(MongoDBObject("parent" -> None, "name" -> pattern.r)).skip(offest).limit(5).toList
      var recTaxonimies = Taxonomy.find(MongoDBObject("parent" -> None, "name" -> pattern.r)).size
      var products = models.Product.find(MongoDBObject("title" -> pattern.r)).skip(offest).limit(5).toList
      var records = models.Product.find(MongoDBObject("title" -> pattern.r)).size
      Ok(views.html.backoffice.catalogPriceRules.conditionspopup(Page(products, page, offest, records), 
          Page(mainTaxonomies, page, offest, recTaxonimies), orderBy, filter, opt, op, tr, ruleCondition))
  }
  
  def selectedProducts(idList: String, startIndex : Int, opt: String, op: String, 
      tr: String, id: String) = Action { implicit request =>
    //println(idList)
    //println("tr="+tr);
    //println("cond id="+id);
    var prodArr = idList.split(",")
    var buf, itemName, itemId, opString, optString = ""
    var x = 0
    
    //println("opt="+opt)
    if(opt.equals("Product")){
      opString = AttributesOperators.productOperators.find(p => p._1 == op).get._2
      optString=opt
    } else if (opt.equals("Categories")){
      opString = AttributesOperators.categoryOperators.find(p => p._1 == op).get._2
      optString="Product "+opt
    } else {
      optString="Product "+opt
    }
    var tagId = "tr"+startIndex
    var conId = new ObjectId
    
    if(!tr.equals("")){      
      conId = new ObjectId(id)
    }    
    
    buf = "<li id='tr"+startIndex+"'><div class='alert alert-info'>" +
    		"<input type='hidden' value='"+conId+"' name='conditions["+startIndex+"].conditionId' id='conditions["+startIndex+"].conditionId'/>" +
    		"<input type='hidden' value='"+opt+"' name='conditions["+startIndex+"].attribute' id='conditions["+startIndex+"].attribute'/>" +
    		"<input type='hidden' value='"+op+"' name='conditions["+startIndex+"].operator' id='conditions["+startIndex+"].operator'/>" +
    		""+optString+" <b>"+opString+"</b> ("
    for (prod <- prodArr){
      if(opt.equals("Product")){
        val productFromDB = models.Product.findOne( MongoDBObject( "_id" -> new ObjectId(prod)) ).getOrElse( null )
        itemName = productFromDB.title
        itemId = productFromDB.id.toString()
    } else if (opt.equals("Categories")){
        val productFromDB = Taxonomy.findOne( MongoDBObject( "_id" -> new ObjectId(prod)) ).getOrElse( null )
        itemName = productFromDB.name
        itemId = productFromDB.id.toString()
    }     
      buf += " "+itemName+""   
      buf += "<input type='hidden' value=\""+itemId+"\" name='conditions["+startIndex+"].products["+x+"].id' id='conditions["+startIndex+"].products["+x+"].id'/>"
      buf += "<input type='hidden' value=\""+itemName+"\" name='conditions["+startIndex+"].products["+x+"].name' id='conditions["+startIndex+"].products["+x+"].name'/>"
      x = x + 1      
      if(prodArr.size == x){
        buf += "."
      } else {
        buf += ","
      }
    }
    buf += ") <a class='btn btn-mini'><i class='icon-pencil icon-white'></i></a>" +
    		" <a class='btn btn-mini btn-danger' onclick=removethis('"+tagId+"')><i class='icon-trash icon-white'></i></a></div></li>"
    Ok(buf)
  }
  
  def addConditionSet(startIndex : Int, opt: String, op: String, v: String,
      tr: String, id: String) = Action { implicit request =>    
    //println("tr="+tr);
    //println("cond id="+id);   
    var buf = ""
    val attrString = AttributesOperators.attributes.find(a => a._1 == opt).get._2
    val opString = AttributesOperators.operators.find(o => o._1 == op).get._2    
    var tagId = "tr"+startIndex
    var conId = new ObjectId
    
    if(!tr.equals("")){      
      conId = new ObjectId(id)
    }    
    
    buf = "<li id='tr"+startIndex+"'><div class='alert alert-info'>" +
    		"<input type='hidden' value='"+conId+"' name='conditions["+startIndex+"].conditionId' id='conditions["+startIndex+"].conditionId'/>" +
    		"<input type='hidden' value='"+opt+"' name='conditions["+startIndex+"].attribute' id='conditions["+startIndex+"].attribute'/>" +
    		"<input type='hidden' value='"+op+"' name='conditions["+startIndex+"].operator' id='conditions["+startIndex+"].operator'/>" +
    		"<input type='hidden' value='"+v+"' name='conditions["+startIndex+"].cvalue' id='conditions["+startIndex+"].cvalue'/>" +
    		"Product "+attrString+" <b>"+opString+"</b> "+v+ ". <a class='btn btn-mini'><i class='icon-pencil icon-white'></i></a>" +
    		"<a class='btn btn-mini btn-danger' onclick=removethis('"+tagId+"')><i class='icon-trash icon-white'></i></a></div></li>"    
    Ok(buf)
  }
  
  def displaySelectedProduct(id: String, opt: String) = Action { implicit request =>    
    var buf = ""
    var item = ""
    if(opt.equals("Product")){
      val productFromDB = models.Product.findOne( MongoDBObject( "_id" -> new ObjectId(id)) ).getOrElse( null )
      item = productFromDB.title
    } else if (opt.equals("Categories")){
      val productFromDB = Taxonomy.findOne( MongoDBObject( "_id" -> new ObjectId(id)) ).getOrElse( null )
      item = productFromDB.name
    }
    buf = "<tr id='tr_"+id+"'>" +
    		"<td style='width: 2%'>" +
    		"<button id='bAdd"+id+"' class='btn btn-mini btn-danger' type='button' onClick=remProd('"+id+"');>" +
    				"<i class='icon-minus-sign icon-white'></i></button>" +
    		"</td>" +
    		"<td style='width: 60%'>"+item+"</td>" +
    		"</tr>"
    Ok(buf)
  }
  
  def displaySelectedProducts(idList: String, opt: String) = Action { implicit request =>
    //println("idList="+idList)
    var buf = ""
    var item = ""
    if(!idList.isEmpty()){  
      var prodArr = idList.split(",")        
      for (prod <- prodArr){
        if(opt.equals("Product")){
          val productFromDB = models.Product.findOne( MongoDBObject( "_id" -> new ObjectId(prod)) ).getOrElse( null )
          item = productFromDB.title
        } else if (opt.equals("Categories")){
          val productFromDB = Taxonomy.findOne( MongoDBObject( "_id" -> new ObjectId(prod)) ).getOrElse( null )
          item = productFromDB.name
        }        
        buf += "<tr id='tr_"+prod+"'>" +
    		"<td style='width: 2%'>" +
    		"<button id='bAdd"+prod+"' class='btn btn-mini btn-danger' type='button' onClick=remProd('"+prod+"');>" +
    				"<i class='icon-minus-sign icon-white'></i></button>" +
    		"</td>" +
    		"<td style='width: 60%'>"+item+"</td>" +
    		"</tr>"
      }
    }
    Ok(buf)
  }
    
  def addCondition(catalogPriceRuleId: String) = Action { implicit request =>     
     val catalogPriceRules = getCatalogPriceRules(new ObjectId(catalogPriceRuleId))
     val ret = """xxx"""     
     Ok(ret)
  }
  
  def lists = Action { implicit request =>    
    var catalogPriceRules = CatalogPriceRules.findAll.toList.sortBy(c => c.priority)    
    catalogPriceRules.foreach(cpr => {
      var customerGroups: List[String] = List.empty
     if (cpr.customerGroups.isDefined) {      
      cpr.customerGroups.get.asInstanceOf[BasicDBList].toList.foreach(
        cg => customerGroups ++= List(cg.asInstanceOf[String]))
      cpr.customerGroups = Some(customerGroups)
    }      
    })  
    
    Ok(views.html.backoffice.catalogPriceRules.list(catalogPriceRules))
  }
  
  def delete(id: ObjectId) = Action { implicit request =>
    CatalogPriceRules.remove(MongoDBObject("_id" -> id))
    Redirect(controllers.backoffice.routes.PriceRules.lists()).flashing("message" -> "Rule deleted !!!")
  }
  
  def updatePriorityOrder(oldIndex: String, newIndex: String) = Action { implicit request => 
    val oI = oldIndex.toInt
    val nI = newIndex.toInt
    val cpr = CatalogPriceRules.findAll.toArray.sortBy(c => c.priority)
    
    if(oI < nI){
      for( a <- oI+1 to nI){
        val toCPR = cpr(a)        
        toCPR.priority = a        
        CatalogPriceRules.save(toCPR)  
      }      
    }else if (oI > nI){
      for( a <- nI to oI-1){        
        val toCPR = cpr(a)        
        toCPR.priority = a+2        
        CatalogPriceRules.save(toCPR)  
      }
    }
    
    val fromCPR = cpr(oI) 
    fromCPR.priority = nI+1
    CatalogPriceRules.save(fromCPR)
    
    var buf = "oldIndex="+oI+":newIndex="+nI    
    Ok(buf)
  }
  
}
