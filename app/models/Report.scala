package models

import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import com.mongodb.casbah.Imports._

case class Report(
  var fromDate: Date = {
    val cal = (Calendar.getInstance)
    cal.add(Calendar.MONTH, -1); cal.getTime
  },
  var toDate: Date = Calendar.getInstance.getTime,
  var name: String = null,
  var reportFilters: Map[String, Filter] = Map.empty,
  var displayFilters: Map[String, Filter] = Map.empty,
  var statusFilters: Map[String, Filter] = Map.empty,
  var scoreBoard: List[Score] = List.empty,
  var csvExports: List[CsvExport] = List.empty,
  var dataList: DataList = null,
  var query: DBObject = DBObject.empty,
  var refined: Boolean = false,
  var isCsv: Boolean = false) {

}

object Report {

  val countryList = buildCountryList()

  private def buildCountryList(): Map[String, String] = {

    val countries = Country.findAll().toList
    var countryMap: Map[String, String] = Map.empty
    countries.map { country =>
      countryMap += (country.code -> country.name)
    }
    countryMap
  }
}

case class Filter(
  var name: String = null,
  var selectedKeyList: String = "",
  var selectedDisplayList: String = "")

case class Score(

  var name: String = null,
  var score: BigDecimal = 0)

case class DataList(

  var recordHeaders: List[String] = null,
  var recordList: List[List[String]] = null)

case class CsvExport(

  var csvType: String = null,
  var csvLabel: String = null)

