package org.cmhh

import scala.util.{Try, Success, Failure}
import java.time.LocalDate

/**
 * Convert date strings found in Excel spreadsheets to LocalDate
 */
object date {
  private def month(m: String): Int = m.toUpperCase match {
    case "JAN" =>  1 
    case "FEB" =>  2 
    case "MAR" =>  3
    case "APR" =>  4
    case "MAY" =>  5
    case "JUN" =>  6
    case "JUL" =>  7
    case "AUG" =>  8
    case "SEP" =>  9
    case "OCT" => 10
    case "NOV" => 11
    case "DEC" => 12
    case _     => sys.error("Invalid argument.")
  }

  private def parse1(dte: String): LocalDate = {
    val pattern = "^(\\d{2})-([a-zA-Z]{3})-(\\d{4})$".r
    val pattern(d, m, y) = dte
    LocalDate.of(y.toInt, month(m), d.toInt)
  }

  private def parse2(dte: String): LocalDate = {
    val pattern = "^([a-zA-Z]{3}) (\\d{4})$".r
    val pattern(m, y) = dte
    LocalDate.of(y.toInt, month(m), 1).plusMonths(1).minusDays(1)
  }

  /**
   * Convert date string to LocalDate
   * 
   * @param dte String
   * @return LocalDate
   */
  def parse(dte: String): LocalDate = {
    if (dte.matches("^(\\d{2})-([a-zA-Z]{3})-(\\d{4})$")) parse1(dte)
    else if (dte.matches("^([a-zA-Z]{3}) (\\d{4})$")) parse2(dte)
    else sys.error(s"Invalid date format ($dte).")
  }
}