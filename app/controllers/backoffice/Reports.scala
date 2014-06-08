package controllers.backoffice
import java.util.Date
import play.api.mvc.Controller
import play.api.mvc.Action
import models._
import scala.collection.immutable.List
import com.mongodb.casbah.Imports._
import com.novus.salat._
import models.salatctx._
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.io.File
import play.api.libs.iteratee.Enumerator
import play.api.mvc.SimpleResult
import play.api.mvc.ResponseHeader
import java.net.URLConnection
import au.com.bytecode.opencsv.CSVWriter
import java.io.FileWriter
import play.api.mvc.Response
import play.api.Play.current
import play.api.Play
import java.util.Calendar

object Reports extends Controller {

  private val formatter = new SimpleDateFormat("MM/dd/yy")

  def show = Action { implicit request =>

    Redirect(controllers.backoffice.routes.Reports.showOrderReport)

  }

  def getFilterDataList(filterName: String) = Action { implicit request =>

    Ok(views.html.backoffice.reports.filterValuesPopUp(
      filterName, getPopUpDataSeq(filterName)))

  }

  // Shows the orders report
  def showOrderReport = Action { implicit request =>

    //Step 1 creating report name
    var report = Report()
    report.name = "Orders"

    // refined requests comes as post
    if (request.method.equals("POST")) {
      report.refined = true
    }

    //Step 2 refine by date
    report = refineByDateCriteria(request, report)

    //Step 3 Creating Report Filters   

    //Shipping Address country Filter
    report = createReportFilter(
      request, report, "Shipping Country", "shipAddresses.country")

    //Billing Address country filter
    report = createReportFilter(
      request, report, "Billing Country", "billAddresses.country")

    //Current Order Status filter
    report = createReportFilter(
      request, report, "Current Order Status", "state")

    //Step 4  Building Report Data List 

    val dataHeaders = List(
      "Date",
      "id",
      "productTax",
      "shipFee",
      "shipTax",
      "subTotal",
      "BillCountry",
      "ShippingCountry")

    val orders = Order.find(report.query.asDBObject).toList.sortBy(c => c.date)

    report.dataList = DataList(dataHeaders, getOrders(orders))

    // Step 5  Setting ScoreBoard Values  

    report.scoreBoard = getOrderScoreBoard(orders)

    // Step 6 set CSV download types

    report.csvExports ::= getCSVExport(report, "csvOrder", "Exports Orders as CSV")

    // Checks whether if its a csv if so proceed

    if (report.refined && request.body.asFormUrlEncoded.get("isCsv").head.toString().toBoolean) {

      report.isCsv = true

      val csvType = request.body.asFormUrlEncoded.get("csvType").head.toString()

      val file = generateCSV(report.dataList, report.name, csvType)

      Ok.sendFile(new java.io.File(file))

    } else {

      Ok(views.html.backoffice.reports.show(report))
    }

  }

  // handled data refinement
  private def refineByDateCriteria(
    implicit request: play.api.mvc.Request[play.api.mvc.AnyContent],
    report: Report): Report = {

    // refined requests comes as post
    if (report.refined) {

      //Start setting report dates
      report.fromDate = formatter.parse(
        request.body.asFormUrlEncoded.get("fromDate").head.toString(),
        new ParsePosition(0))

      val cal = (Calendar.getInstance)
      cal.setTime(formatter.parse(
        request.body.asFormUrlEncoded.get("toDate").head.toString(),
        new ParsePosition(0)))

      cal.add(Calendar.HOUR, +23)
      cal.add(Calendar.MINUTE, +59)
      cal.add(Calendar.SECOND, +59)

      report.toDate = cal.getTime

    }

    report.query ++= ("date" $lte report.toDate $gte report.fromDate)

    report
  }

  //Creates and returns a report filter
  private def createReportFilter(
    implicit request: play.api.mvc.Request[play.api.mvc.AnyContent],
    report: Report,
    filterName: String,
    dbField: String): Report = {

    val newfilter: Filter = Filter()
    newfilter.name = filterName

    if (report.refined) {

      val strSelectedKeyList =
        request.body.asFormUrlEncoded.get(
          "selectedFilterList_" + filterName).head.toString()
      val strSelectedDisplayList =
        request.body.asFormUrlEncoded.get(
          "selectedFilterDisplayList_" + filterName).head.toString()

      if (!strSelectedKeyList.isEmpty()) {

        var selectedKeyList: List[String] = List()

        for (code <- strSelectedKeyList.split(",")) {
          selectedKeyList ::= code
        }

        report.query ++= (dbField $in selectedKeyList)

        newfilter.selectedKeyList = strSelectedKeyList
        newfilter.selectedDisplayList = strSelectedDisplayList

      }
    }

    report.reportFilters += (filterName -> newfilter)

    report

  }

  // Selects data set for each filter popup
  // add entry to case each time adding a new filter.Related data return 
  // method should be created also.

  private def getPopUpDataSeq(filterName: String): Seq[(String, String)] = {

    filterName match {
      case "Shipping Country" =>
        return getCountryPopUpSeq
      case "Billing Country" =>
        return getCountryPopUpSeq
      case "Current Order Status" =>
        return getOrderStatusPopUpSeq
    }

  }

  // Returns Seq of country data to show in pop up
  // 
  private def getCountryPopUpSeq(): Seq[(String, String)] = {

    var popUpDataSeq: Seq[(String, String)] = Seq()
    val countries: List[Country] = getActiveCountries

    countries.map { country =>
      popUpDataSeq :+= (country.code, country.name)
    }

    popUpDataSeq
  }

  // Returns Seq of order state data to show in pop up
  // 
  private def getOrderStatusPopUpSeq(): Seq[(String, String)] = {

    var popUpDataSeq: Seq[(String, String)] = Seq()

    OrderState.values.foreach(value =>

      popUpDataSeq :+= (value.toString(), value.toString()))

    popUpDataSeq
  }

  private def getOrders(orders: List[Order]): List[List[String]] = {

    var records: List[List[String]] = List()

    orders.map { order =>

      records ::= List(
        new SimpleDateFormat("M/d/yyyy") format order.date.get.getTime,
        order.id.toString(),
        order.productTax.toString,
        order.shipFee.toString,
        order.shipTax.toString,
        order.subTotal.toString,

        if (order.billAddresses.isDefined
          && order.billAddresses.get.country.isDefined) {
          Report.countryList.get(order.billAddresses.get.country.get).get
        } else { "" },

        if (order.shipAddresses.isDefined
          && order.shipAddresses.get.country.isDefined) {
          Report.countryList.get(order.shipAddresses.get.country.get).get
        } else { "" })

    }
    records
  }

  private def getActiveCountries(): List[Country] = {

    (Country.find(MongoDBObject("enabledInAA" -> true)).toList.sortBy(c => c.name))

  }

  private def getOrderScoreBoard(orders: List[Order]): List[Score] = {

    var revenue: BigDecimal = 0
    var total: BigDecimal = 0
    var tax: BigDecimal = 0
    var shipping: BigDecimal = 0

    orders.map { order =>
      total += order.subTotal + order.shipFee + order.shipTax
      shipping += order.shipFee + order.shipTax

      order.lineItems.map { lineItem =>
        tax += lineItem.tax
        revenue += (lineItem.price * lineItem.qty)
      }

    }

    var scores: List[Score] = List.empty
    scores ::= Score("Shipping", shipping)
    scores ::= Score("Tax", tax)
    scores ::= Score("Total", total)
    scores ::= Score("Revenue", revenue)
    scores ::= Score("Orders", orders.length)

    return scores
  }

  //Generate CSV file from a given dataset and reportname
  private def generateCSV(dataList: DataList,
    reportName: String,
    recordListName: String): String = {

    val filePath = Play.application.path + "/public/temp/csvExports/"

    val fileName =
      reportName +
        "_Report_" +
        recordListName +
        "_" +
        new SimpleDateFormat("yyyyMMddHHmmss").format(new Date) +
        ".csv"

    val writer: CSVWriter = new CSVWriter(new FileWriter(filePath + fileName), ',')

    writer.writeNext(dataList.recordHeaders.toArray)
    dataList.recordList.foreach(
      record => writer.writeNext(record.toArray))
    writer.close();

    return (filePath + fileName)

  }

  // Creates a Csv export Link
  private def getCSVExport(
    report: Report,
    csvType: String,
    csvLabel: String): CsvExport = {

    val csvExport = CsvExport()

    csvExport.csvLabel = csvLabel

    csvExport.csvType = csvType

    csvExport
  }

}

