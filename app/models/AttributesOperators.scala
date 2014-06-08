package models

object AttributesOperators {
  
  val attributes = List(
    Tuple2("Price","Base Price"),
    Tuple2("Categories","Categories"),
    Tuple2("Product","Product")
    /*<option value="Shop_ProductAttribute_Condition:price">Base Price</option>
	  <option value="Shop_ProductAttribute_Condition:categories">Categories</option>
	  <option value="Shop_ProductAttribute_Condition:depth">Depth</option>
	  <option value="Shop_ProductAttribute_Condition:height">Height</option>
	  <option value="Shop_ProductAttribute_Condition:description">Long Description</option>
	  <option value="Shop_ProductAttribute_Condition:manufacturer_link">Manufacturer</option>
	  <option value="Shop_ProductAttribute_Condition:name">Name</option>
	  <option value="Shop_ProductAttribute_Condition:current_price">Price</option>
	  <option value="Shop_ProductAttribute_Condition:product">Product</option>
	  <option value="Shop_ProductAttribute_Condition:product_type">Product Type</option>
	  <option value="Shop_ProductAttribute_Condition:on_sale">Product is on Sale</option>
	  <option value="Shop_ProductAttribute_Condition:sku">SKU</option>
	  <option value="Shop_ProductAttribute_Condition:short_description">Short Description</option>
	  <option value="Shop_ProductAttribute_Condition:tax_class">Tax Class</option>
	  <option value="Shop_ProductAttribute_Condition:weight">Weight</option>
	  <option value="Shop_ProductAttribute_Condition:width">Width</option>*/
  )
  
  val conditionList = List(
    Tuple2("Shop_RuleIfCondition","Compound condition")
  )
  
  val operators = List(
    Tuple2("=","is"),
    Tuple2("!=","is not"),
    Tuple2(">=","equals or greater than"),
    Tuple2("<=","equals or less than"),
    Tuple2(">","greater than"),
    Tuple2("<","less than")
  )
  
  val productOperators = List(
    Tuple2("one_of","is one of"),
    Tuple2("not_one_of","is not one of")
  )
  
  val categoryOperators = List(
    Tuple2("=","is"),
    Tuple2("!=","is not"),
    Tuple2("one_of","is one of"),
    Tuple2("not_one_of","is not one of")
  )
  
  /*<li id="Shop_CatalogPriceRule_action_class_name_chzn_o_0" class="active-result" style="">By fixed amount</li>
    <li id="Shop_CatalogPriceRule_action_class_name_chzn_o_1" class="result-selected active-result" style="">By percentage of the original price</li>
    <li id="Shop_CatalogPriceRule_action_class_name_chzn_o_2" class="active-result" style="">To fixed price</li>
    <li id="Shop_CatalogPriceRule_action_class_name_chzn_o_3" class="active-result" style="">To percentage of the original price</li>*/
  
  val actions = List(
    Tuple2("by_fixed_amount","By fixed amount"),
    Tuple2("by_percentage","By percentage of the original price"),
    Tuple2("to_fixed_price","To fixed price"),
    Tuple2("to_percentage","To percentage of the original price")
  )
           
      
}