package helpers

import java.net.URLDecoder
import java.io._
import java.net.URL
import java.net.URLEncoder
import scala.Boolean
import models._
import scala.collection.immutable.HashMap

object PaymentManager {

  val apiUrl = PaymentCredentials.apiUrl
  val user = PaymentCredentials.user
  val pwd = PaymentCredentials.pwd
  val VERSION = PaymentCredentials.version
  val signature = PaymentCredentials.signature

  def getTransactionDetails(tx: String): HashMap[String, Any] = {

    var requestParameters = "Method=" + URLEncoder.encode("GetTransactionDetails", "UTF-8")
    requestParameters += "&VERSION=" + VERSION
    requestParameters += "&USER=" + URLEncoder.encode(user, "UTF-8")
    requestParameters += "&PWD=" + URLEncoder.encode(pwd, "UTF-8")
    requestParameters += "&SIGNATURE=" + URLEncoder.encode(signature, "UTF-8")
    requestParameters += "&TRANSACTIONID=" + URLEncoder.encode(tx, "UTF-8")

    var strData = URLDecoder.decode(callHttp(apiUrl, requestParameters), "UTF-8")

    val gatewayResponse = strData.split('&') map { str =>
      val pair = str.split('=')
      (pair(0) -> pair(1))
    } toMap

    //    gatewayResponse.map(value => {
    //      println(value._1 + " : " + value._2)
    //    })

    var transactionDetailsRs = HashMap[String, Any]()
    transactionDetailsRs += ("success" -> false)

    if (gatewayResponse.get("ACK").isDefined
      && gatewayResponse.get("ACK").get.equals("Success")
      && gatewayResponse.get("PAYMENTSTATUS").isDefined
      && gatewayResponse.get("PAYMENTSTATUS").get.equals("Completed")
      && gatewayResponse.get("CURRENCYCODE").isDefined
      && gatewayResponse.get("AMT").isDefined) {

      transactionDetailsRs += (
        ("INVNUM" -> gatewayResponse.get("INVNUM").get),
        ("paymentCurrencyTotal" -> gatewayResponse.get("AMT").get.toDouble),
        ("paymentCurrency" -> gatewayResponse.get("CURRENCYCODE").get),
        ("success" -> true))
    }
    return transactionDetailsRs
  }

  private def callHttp(url: String, requestParameters: String) = {

    val conn = new URL(url).openConnection
    val HttpRequestTimeout = 15000
    conn.setConnectTimeout(HttpRequestTimeout)
    conn.setDoOutput(true)
    conn.connect

    val wr = new OutputStreamWriter(conn.getOutputStream())
    wr.write(requestParameters)
    wr.flush
    wr.close

    var br = new BufferedReader(new InputStreamReader(conn.getInputStream()))
    var read = br.readLine()
    var sb = new StringBuilder()

    while (read != null) {
      sb.append(read)
      read = br.readLine()
    }
    sb.toString()
  }

}