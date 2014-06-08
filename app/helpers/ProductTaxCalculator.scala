package helpers

import models._
import com.mongodb.casbah.Imports._
import com.novus.salat._
import controllers.backoffice.TaxRates
import models.LineItem
import models.Rate
import models.Product

/** Handles tax calculation for individual products.
 *
 */

object ProductTaxCalculator {

  var shippingCountry, shippingState, shippingCity, shippingZip = ""

  var compound = false

  def addTax(order: Order): Order = {

    val shippingAddress = order.shipAddresses.get

    shippingCountry = shippingAddress.country.getOrElse("")
    shippingState = shippingAddress.state.getOrElse("")
    shippingCity = shippingAddress.city.getOrElse("").trim()
    shippingZip = shippingAddress.postCode.getOrElse("").trim()

    //totalProductTax holds total product tax for an order
    var lineItemTax, totalProductTax: BigDecimal = 0
    var taxedLineItems: List[LineItem] = List.empty

    var subTotal: BigDecimal = 0
	
    order.lineItems.map {
      lineItem =>

        lineItemTax = getTax(lineItem.product, lineItem.price, lineItem.qty)

        totalProductTax += lineItemTax

        lineItem.tax = lineItemTax
        taxedLineItems ::= lineItem
		System.out.println(lineItem.price)
        subTotal += (lineItem.price * lineItem.qty) + lineItemTax
    }

    order.lineItems = taxedLineItems
    order.productTax = totalProductTax
    order.subTotal = subTotal

    return order
  }

  private def getTax(id: ObjectId,
    priceBeforeTax: BigDecimal, qty: Int): BigDecimal = {

    var listOne, listTwo: List[models.Rate] = List.empty

    val taxClassId = models.Product.findOneById(id).get
      .taxClassId.getOrElse({ return 0 })

    models.Tax.findOneById(new ObjectId(taxClassId))
      .getOrElse({ return 0.00 }).rate.foreach(taxRate => {

        if (taxRate.priority == 1) { listOne ::= taxRate }
        else if (taxRate.priority == 2) { listTwo ::= taxRate }

      })

    val total = priceBeforeTax * qty

    // Calculating first priority taxes 
    val priorityOneTaxRate = traverseRates(listOne)
    val priorityOneTax = total * (priorityOneTaxRate / 100)

    // Calculating second priority taxes
    val priorityTwoTaxRate = traverseRates(listTwo)
    var priorityTwoTax: BigDecimal = 0.00

    if (compound) {
      priorityTwoTax = (priorityOneTax + total) * (priorityTwoTaxRate / 100)

    } else {
      priorityTwoTax = total * (priorityTwoTaxRate / 100)

    }

    return (priorityOneTax + priorityTwoTax)

  }

  private def traverseRates(rates: List[models.Rate]): BigDecimal = {
    compound = false

    rates.foreach(taxRate => {

      if (taxRate.country.equalsIgnoreCase(shippingCountry)
        && taxRate.state.equalsIgnoreCase(shippingState)
        && taxRate.city.isDefined
        && taxRate.city.get.equalsIgnoreCase(shippingCity)
        && taxRate.zipCode.isDefined
        && taxRate.zipCode.get.equalsIgnoreCase(shippingZip)) {

        if (taxRate.compound.isDefined) { compound = taxRate.compound.get }
        return taxRate.rate

      }

      if (taxRate.country.equals(shippingCountry)
        && taxRate.state.equals(shippingState)
        && taxRate.city.isDefined
        && taxRate.city.get.equalsIgnoreCase(shippingCity)) {

        if (taxRate.compound.isDefined) { compound = taxRate.compound.get }
        return taxRate.rate

      }

      if ((taxRate.country.equals(shippingCountry)
        && taxRate.state.equals(shippingState))) {

        if (taxRate.compound.isDefined) { compound = taxRate.compound.get }
        return taxRate.rate

      }

      if ((taxRate.country.equals(shippingCountry)
        && taxRate.state.equals("*"))) {

        if (taxRate.compound.isDefined) { compound = taxRate.compound.get }
        return taxRate.rate

      }

      if (taxRate.country.equals("*")
        && taxRate.state.equals("*")) {

        if (taxRate.compound.isDefined) { compound = taxRate.compound.get }
        return taxRate.rate

      }

    })
    return 0
  }

}