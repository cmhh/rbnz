package org.cmhh

import org.openqa.selenium.{WebDriver, WebElement, By, Dimension}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import java.util.concurrent.TimeUnit
import java.io.File
import java.time.{ZoneId, ZonedDateTime}
import scala.util.{Try, Success, Failure}

object browser {
  private def now: ZonedDateTime = {
    ZonedDateTime
      .now()
      .withZoneSameInstant(ZoneId.of("Pacific/Auckland"))
  }

  lazy val userAgent: String = {
    java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.OFF)
    System.setProperty("webdriver.chrome.verboseLogging", "false")
    System.setProperty("webdriver.chrome.silentOutput", "true")

    val options: ChromeOptions = new ChromeOptions()
    options.addArguments("--window-size=1920,1080")
    options.addArguments("--headless")
    options.addArguments("--no-sandbox")
    options.addArguments("--disable-extensions")
    options.addArguments("--disable-in-process-stack-traces")
    options.addArguments("--disable-logging")
    options.addArguments("--disable-dev-shm-usage")
    options.addArguments("--log-level=0")
    options.addArguments("--silent")
    options.addArguments("--output=/dev/null")
    options.addArguments("--disable-blink-features=AutomationControlled")
    options.setExperimentalOption("excludeSwitches", Array("enable-automation"))

    val driver = new ChromeDriver(options)
    val res = driver.executeScript("return navigator.userAgent;").toString()
    driver.close()
    res
  }

  def getDriver(workdir: String): ChromeDriver = {
    val wrk = new File(workdir)

    val options: ChromeOptions = new ChromeOptions()
    options.addArguments("--window-size=1920,1080")
    options.addArguments("--headless")
    options.addArguments("--no-sandbox")
    options.addArguments("--disable-extensions")
    options.addArguments("--disable-in-process-stack-traces")
    options.addArguments("--disable-logging")
    options.addArguments("--disable-dev-shm-usage")
    options.addArguments("--log-level=0")
    options.addArguments("--silent")
    options.addArguments("--output=/dev/null")
    options.addArguments("--disable-blink-features=AutomationControlled")
    options.addArguments(s"""user-agent=${userAgent.replace("HeadlessChrome", "Chrome")}""")
    options.setExperimentalOption("excludeSwitches", Array("enable-automation"))

    val prefs = new java.util.HashMap[String, Any]()
    prefs.put("download.prompt_for_download", false)
    prefs.put("download.default_directory", wrk.getAbsolutePath())

    options
      .setExperimentalOption(
        "prefs", 
        prefs
      )

    val driver = new ChromeDriver(options)
    driver.manage().timeouts().implicitlyWait (10, TimeUnit.SECONDS)
    driver
  }
}