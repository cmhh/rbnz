package org.cmhh

import java.io.File
import org.rogach.scallop._

class DownloadDataConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val downloadDir = opt[String](name = "download-dir", required = false, default = Some("downloads"))
  verify()
}

class CreateDatabaseConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val downloadDir = opt[String](name = "download-dir", required = false, default = Some("downloads"))
  val dbPath = opt[String](name = "database-path", required = false, default = Some("output/rbnz.sqlite"))
  verify()
}

/**
 * Download Excel spreadsheets for RBNZ website and store locally
 */
object DownloadData extends App {
  val conf = new DownloadDataConf(args.toIndexedSeq)
  rbnz.download(conf.downloadDir())
}

/**
 * Build SQLite database from Excel files on RBNZ website.
 */
object CreateDatabase extends App {
  val conf = new CreateDatabaseConf(args.toIndexedSeq)
  val dbpath = new File(conf.dbPath())
  if (dbpath.exists) dbpath.delete()
  val conn = db.connect(conf.dbPath())
  db.loadAll(conn, conf.downloadDir())
  conn.close()
}