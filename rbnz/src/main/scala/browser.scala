package org.cmhh

import org.openqa.selenium.{WebDriver, WebElement, By, Dimension}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import java.util.concurrent.TimeUnit
import java.io.File
import java.time.{ZoneId, ZonedDateTime, Duration}
import scala.util.{Try, Success, Failure}

// import io.github.bonigarcia.wdm.WebDriverManager

object browser {
  private def now: ZonedDateTime = {
    ZonedDateTime
      .now()
      .withZoneSameInstant(ZoneId.of("Pacific/Auckland"))
  }

  lazy val userAgent: String = {
    // WebDriverManager.chromedriver().setup()

    java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.OFF)
    System.setProperty("webdriver.chrome.verboseLogging", "false")
    System.setProperty("webdriver.chrome.silentOutput", "true")

    val options: ChromeOptions = new ChromeOptions()
    options.addArguments("--no-sandbox")
    options.addArguments("--disable-dev-shm-usage")
    options.addArguments("--headless=new") // headless=new
    options.addArguments("--disable-gpu")
    options.addArguments("--remote-debugging-port=9222")
    options.addArguments("--disable-extensions")
    options.addArguments("--disable-in-process-stack-traces")
    options.addArguments("--disable-logging")
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

    // WebDriverManager.chromedriver().setup()

    val options: ChromeOptions = new ChromeOptions()
    options.addArguments("--no-sandbox")
    options.addArguments("--disable-dev-shm-usage")
    options.addArguments("--headless=new")
    options.addArguments("--window-size=1920,1080")
    options.addArguments("--disable-extensions")
    options.addArguments("--disable-in-process-stack-traces")
    options.addArguments("--disable-logging")
    options.addArguments("--log-level=0")
    options.addArguments("--silent")
    options.addArguments("--output=/dev/null")
    options.addArguments("--disable-blink-features=AutomationControlled")
    options.addArguments(s"""user-agent=${userAgent.replace("HeadlessChrome", "Chrome")}""")
    options.setExperimentalOption("excludeSwitches", Array("enable-automation"))
    options.setExperimentalOption("useAutomationExtension", false)

    val prefs = new java.util.HashMap[String, Any]()
    prefs.put("profile.default_content_settings.popups", 0)
    prefs.put("download.prompt_for_download", false)
    prefs.put("download.directory_upgrade", true)
    prefs.put("download.default_directory", wrk.getAbsolutePath())

    options
      .setExperimentalOption(
        "prefs", 
        prefs
      )

    val driver = new ChromeDriver(options)
    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)// (Duration.ofSeconds(10)) // (10, TimeUnit.SECONDS)
    driver
  }
}