package helpers

import play.api._
import play.api.mvc.Request
import models._
import java.math.MathContext
import java.math.RoundingMode._

object CurrencyConvertor {

  def getRate(request: play.api.mvc.RequestHeader, rate: BigDecimal): BigDecimal = {
  	//System.out.println(request.session.get("cRate"));
    var conRate = request.session.get("cRate").getOrElse(1.0).toString().toDouble   
    (rate * conRate).setScale(2, BigDecimal.RoundingMode.CEILING)    
  }

  def getSymbol(request: play.api.mvc.RequestHeader): String = {
    request.session.get("cSymbol").getOrElse(Currency.baseCurrency.symbol).toString()
  }
  
  def getBaseCurrencySymbol(request: play.api.mvc.RequestHeader): String = {
    request.session.get("bCSymbol").getOrElse(Currency.baseCurrency.symbol).toString()    
  }
  
  def getCode(request: play.api.mvc.RequestHeader): String = {
    request.session.get("cCode").getOrElse(Currency.baseCurrency.iso).toString()
  }
  
  def getExRate(request: play.api.mvc.RequestHeader): BigDecimal = {
    request.session.get("cRate").getOrElse(1.0).toString().toDouble     
  }

}
