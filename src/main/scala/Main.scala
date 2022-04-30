package org.cmhh

import java.io.File

/**
 * Download Excel spreadsheets for RBNZ website and store locally
 */
object DownladData extends App {
  if (args.size == 0) {
    println("Usage: java -jar rbnz.jar org.cmhh.DownloadData <path>.")
  } else {
    rbnz.download(args(0))
  }
}

/**
 * Build SQLite database from Excel files on RBNZ website.
 */
object CreateDatabase extends App {
  if (args.size == 0) {
    println("Usage: java -jar rbnz.jar org.cmhh.CreateDatabase <path>.")
  } else {
    val dbpath = new File(args(0))
    if (dbpath.exists) dbpath.delete()
    val conn = db.connect(args(0))
    db.loadAll(conn, "data")
    conn.close()
  }
}