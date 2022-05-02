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
      .findElement(By.cssSelector(".rbnz-webtable-tablecode strong"))
      .getText()

    val desc = row
      .findElements(By.cssSelector(".col-sm-12.col-md-6"))
      .get(0)
      .findElement(By.tagName("a"))
      .getText()

    val links = row
      .findElements(By.cssSelector(".col-sm-12.col-md-6"))
      .get(1)
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
    val wrk = new File(workdir)
    val options: ChromeOptions = new ChromeOptions()
    options.addArguments("--headless")
    options.addArguments("--no-sandbox")
    options.addArguments("--disable-extensions")
    options.addArguments("--disable-blink-features=AutomationControlled")
    options.addArguments("user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36")
    options.setExperimentalOption("excludeSwitches", Array("enable-automation"))
    val prefs = new java.util.HashMap[String, Any]()
    prefs.put("download.prompt_for_download", false)
    prefs.put("download.default_directory", wrk.getAbsolutePath())

    options
      .setExperimentalOption(
        "prefs", 
        prefs
      )

    val driver: ChromeDriver = new ChromeDriver(options)

    driver.get("https://www.rbnz.govt.nz/statistics/")
    driver.manage().timeouts().implicitlyWait (10, TimeUnit.SECONDS)

    // ensure window is wide enough that table rows are displayed in full
    driver.manage().window().maximize()

    // expand all panels 
    driver.findElementById("toggleAccordion").click()

    // get all the links
    val links = driver
      .findElementsByCssSelector(".panel-body  div.col-xs-12.flushed")
      .asScala
      .map(row => processRow(row))
      .toList

    //download all the links
    links.foreach(x => {
      x._3.foreach(y => {
        val f = new File(s"$wrk/${y._2.split('?')(0).split('/').last}")
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
        else loop(row, pos + 1, accum :+ row.getCell(pos).getStringCellValue())
      }
      loop(ws.getRow(4), 1, Vector.empty)
    }

    val dates = {
      def loop(pos: Int, accum: Vector[LocalDate]): Vector[LocalDate] = {
        if (ws.getRow(pos) == null) accum 
        else {
          val c = ws.getRow(pos).getCell(0)
          if (c == null) accum
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
          else (dates(j), Some(c.getNumericCellValue()))
        }
      }).toVector
      (ids(i) -> Series(obs))
    }).toMap

    wb.close()
    excel.close()
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
      else {
        var row = ws.getRow(pos)

        val seriesDef = SeriesDefinition(
          row.getCell(0).toString, row.getCell(1).toString, 
          row.getCell(2).toString, row.getCell(3).toString,
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
    excel.close()
    res
  }
}