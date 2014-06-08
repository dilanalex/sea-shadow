package helpers

import models._
import com.mongodb.casbah.Imports._
import com.novus.salat._
import controllers.backoffice.TaxRates
import models.LineItem
import models.Rate
import models.Product
import models.Rate
import controllers.backoffice.ShippingOptions

/** Handles tax calculation for individual products.
 *
 */

object ShippingTaxCalculator {

  var shippingCountry, shippingState = ""

  def addTax(order: Order): Order = {

    val shippingTableRate = ShippingTableRate
      .findOneById(order.shipMethodId.get).get

    if (shippingTableRate.taxableFlag.isDefined) {

      val shippingAddress = order.shipAddresses.get

      shippingCountry = shippingAddress.country.getOrElse("")
      shippingState = shippingAddress.state.getOrElse("")

      val taxClass = Tax.findOne(MongoDBObject("name" -> ("Shipping")))
        .getOrElse({ order.shipTax = 0.00; return order })

      order.shipTax = order.shipFee * (traverseRates(taxClass.rate) / 100)
    }
    return order
  }

  private def traverseRates(rates: List[models.Rate]): BigDecimal = {

    rates.foreach(taxRate => {

      if ((taxRate.country.equals(shippingCountry)
        && taxRate.state.equals(shippingState))) {

        return taxRate.rate

      }

      if ((taxRate.country.equals(shippingCountry)
        && taxRate.state.equals("*"))) {

        return taxRate.rate

      }

      if (taxRate.country.equals("*")
        && taxRate.state.equals("*")) {

        return taxRate.rate

      }

    })
    return 0
  }

}