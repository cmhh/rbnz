package org.cmhh

import org.openqa.selenium.{WebDriver, WebElement, By}
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.util.concurrent.TimeUnit
import java.io.{File, BufferedWriter, FileWriter, FileInputStream}
import java.time.LocalDate
import scala.io.Source
import scala.util.{Try, Success, Failure}
import scala.jdk.CollectionConverters._
import org.apache.poi.ss.usermodel._
import org.apache.poi.xssf.usermodel.{XSSFWorkbook, XSSFSheet, XSSFRow, XSSFCell}

/**
 * Utilities for working with RBNZ website.
 */
object rbnz {
  private def processRow(row: WebElement): (String, String, List[(String, String)]) = {
    val code = row
      .findElement(By.cssSelector(":nth-child(3)"))
      .getText()
      .toUpperCase

    val desc = row
      .findElement(By.cssSelector(":nth-child(1)"))
      .getText()

    val links = row
      .findElements(By.cssSelector(":nth-child(4)"))
      .get(0)
      .findElements(By.tagName("a"))
      .asScala
      .map(x => (x.getText(), x.getAttribute("href")))
      .toList
      .filter(_._2.toLowerCase.contains("xlsx"))

    (code, desc, links)
  }

  private def getFileName(url: String, prefix: Option[String] = None): String = {
    val f = url.split('/').last.split('?').head
    prefix match {
      case None => f
      case Some(p) => s"$p/$f"
    }
  }

  /**
   * Download files from RBNZ website.
   * 
   * @param workdir folder to store downloaded files.
   * @return object describing each discovered link, including path to downloaded file
   */
  def download(workdir: String): List[(String, String, List[(String, String, String)])] = {
    val driver = browser.getDriver(workdir)

    driver.get("https://www.rbnz.govt.nz/statistics/series/data-file-index-page")

    val rows = driver
      .findElementsByCssSelector(".table-wrapper tbody > tr")
      .asScala

    // get all the links
    val links = rows
      .map(row => processRow(row))
      .toList

    //download all the links
    links.foreach(x => {
      x._3.foreach(y => {
        val f = new File(s"$workdir/${y._2.split('?')(0).split('/').last}")
        if (!f.exists) {
          println(s"${y._2}...")
          driver.get(y._2)
          Thread.sleep(60000) // To satisfy RBNZ terms of service.
        } 
      })
    })

    driver.close()

    links.map(l => {
      val xs = l._3
        .map(x => (x._1, x._2, getFileName(x._2, Some(workdir))))
        .filter(x => {
          new File(x._3).exists
        })

      (l._2, l._2, xs)
    })
  }

  /**
   * Import Data tab of Excel spreadsheet.
   * 
   * @param file path to Excel file
   * @return A map containing time series data, if successful.
   */
  def importXlsxData(file: String): Try[Map[String, Series]] = Try {
    val excelFile: FileInputStream = new FileInputStream(new File(file))
    val wb: XSSFWorkbook = new XSSFWorkbook(excelFile)
    val ws: XSSFSheet = wb.getSheet("Data")

    val ids = {
      def loop(row: XSSFRow, pos: Int, accum: Vector[String]): Vector[String] = {
        if (row.getCell(pos) == null) accum // i feel dirty and i need a shower
        else loop(row, pos + 1, accum :+ row.getCell(pos).getStringCellValue().toUpperCase)
      }
      loop(ws.getRow(4), 1, Vector.empty)
    }

    val dates = {
      def loop(pos: Int, accum: Vector[LocalDate]): Vector[LocalDate] = {
        if (ws.getRow(pos) == null) accum 
        else {
          val c = ws.getRow(pos).getCell(0)
          if (c == null) accum
          else if (c.toString.trim() == "") accum
          else loop(pos + 1, accum :+ date.parse(c.toString))
        }
      }
      loop(5, Vector.empty)
    }

    val res = (0 until ids.size).map(i => {
      val obs = (0 until dates.size).map(j => {
        if (ws.getRow(j + 5) == null) (dates(j), None)
        else {
          val c = ws.getRow(j + 5).getCell(i + 1)
          if (c == null) (dates(j), None)
          else if (c.toString().trim() == "-") (dates(j), None)
          else {
            c.getCellType() match {
              case CellType.NUMERIC => (dates(j), Some(c.getNumericCellValue()))
              case _ => {
                Try {c.toString.trim.toDouble} match {
                  case Success(x) => (dates(j), Some(x))
                  case Failure(e) => (dates(j), None)
                }
              }
            }
          }
        }
      }).toVector
      (ids(i) -> Series(obs))
    }).toMap

    wb.close()
    excelFile.close()
    res
  }

  /**
   * Import "Series Definitions" tab of Excel spreadsheet.
   * 
   * @param file path to Excel file
   * @return A map containing time series definitions, if successful.
   */
  def importXlsxSeriesDefinition(file: String): Try[Map[String, SeriesDefinition]] = Try {
    val excelFile: FileInputStream = new FileInputStream(new File(file))
    val wb: XSSFWorkbook = new XSSFWorkbook(excelFile)
    val ws: XSSFSheet = wb.getSheet("Series Definitions")

    def loop(pos: Int, accum: Vector[SeriesDefinition]): Vector[SeriesDefinition] = {
      if (ws.getRow(pos) == null) accum 
      else if (ws.getRow(pos).getCell(0) == null) accum
      else if (ws.getRow(pos).getCell(0).toString.trim() == "") accum
      else {
        val row = ws.getRow(pos)

        val seriesDef = SeriesDefinition(
          row.getCell(0).toString, row.getCell(1).toString, 
          row.getCell(2).toString.toUpperCase, 
          row.getCell(3) match {
            case null => None
            case x => Some(x.toString)
          },
          row.getCell(4) match {
            case null => None
            case x => Some(x.toString)
          }
        )

        loop(pos + 1, accum :+ seriesDef)
      }
    }

    val res = loop(1, Vector.empty)
      .map(x => (x.seriesId -> x))
      .toMap

    wb.close()
    excelFile.close()
    res
  }
}