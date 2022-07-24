package org.cmhh

import java.io.File
import java.sql.{Connection, DatabaseMetaData, DriverManager, ResultSet}
import scala.util.{Try, Success, Failure}

/**
 * Functions for creating and working with SQLite datbase
 */
object db {
  private def esc(str: String): String = str.replaceAll("'", "''")

  private def tableExists(conn: Connection, table: String): Boolean = {
    def loop(rs: ResultSet): Boolean = {
      if (!rs.next()) false
      else {
        if (rs.getString(3).toUpperCase == table.toUpperCase) true
        else loop(rs)
      }
    }

    loop(conn.getMetaData().getTables(null, null, "%", null))
  }

  private def definitionExists(conn: Connection, id: String): Boolean = {
    val query = s"select sum(1) as n from series_definition where id = '$id'"
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(query)
    if (rs.next()) rs.getInt(1) == 1 else false
  }

  /**
   * Create database connection.
   * 
   * @param path path to SQLite database
   * @return Connection
   */
  def connect(path: String): Connection = {
    Class.forName("org.sqlite.JDBC")
    DriverManager.getConnection(s"jdbc:sqlite:${new File(path).getAbsolutePath()}")
  }

  /**
   * Copy a SQLite database into a memory and return connection.
   *
   * @param path path to SQLite database
   * @return Connection
   */
  def copyToTempDB(path: String): Connection = {
    Class.forName("org.sqlite.JDBC")
    val conn = DriverManager.getConnection(s"jdbc:sqlite:")
    val stmnt = conn.createStatement()
    stmnt.executeUpdate(s"restore from ${new File(path).getAbsolutePath()}")
    stmnt.close()
    conn
  }

  /**
   * Create table 'series' if it doesn't exist.
   * 
   * @param conn database connection
   */
  def createSeriesTable(conn: Connection): Unit = {
    if (!tableExists(conn, "series_definition"))
      createSeriesDefinitionTable(conn)

    val createQuery = """
      create table if not exists series(
        id varchar,
        date varchar,
        value real,
        FOREIGN KEY(id) REFERENCES series_definition(id)
      )
    """

    conn.setAutoCommit(false)
    val stmt = conn.createStatement()
    stmt.execute(createQuery)
    stmt.execute("create index idx_series_1 on series(id)")
    conn.commit()
    stmt.closeOnCompletion()
  }

  /**
   * Create table 'series_definition' if it doesn't exist.
   * 
   * @param conn database connection
   */
  def createSeriesDefinitionTable(conn: Connection): Unit = {
    val createQuery = """
      create table if not exists series_definition(
        "group" varchar,
        id varchar primary key,
        name varchar,
        unit varchar,
        frequency char(1),
        note varchar
      )
    """

    conn.setAutoCommit(false)
    val stmt = conn.createStatement()
    stmt.execute(createQuery)
    stmt.execute("""create index idx_series_definition_1 on series_definition("group", id)""")
    conn.commit()
    stmt.closeOnCompletion()
  }

  /**
   * Load data to 'series' and 'series_definition' tables.
   * 
   * @param conn database connection
   * @param series object holding series information
   * @param definition object holding series definition
   */
  def loadSeries(conn: Connection, series: Series, definition: SeriesDefinition): Unit = {
    conn.setAutoCommit(false)
    val stmt = conn.createStatement()

    val id = definition.seriesId
    val freq = series.freq.toString

    val unitStr = definition.unit match {
      case None => "NULL"
      case Some(u) => s"'${esc(u)}'"
    }

    val noteStr = definition.note match {
      case None => "NULL"
      case Some(n) => s"'${esc(n)}'"
    }

    if (!definitionExists(conn, id)) {
      val defs_query = s"""
        insert into series_definition values (
          '${esc(definition.group)}',
          '${id.toUpperCase}',
          $unitStr,
          '${definition.unit}',
          '$freq',
          $noteStr
        )
      """

      stmt.execute(defs_query)
      conn.commit()
    }

    series.data.foreach(obs => {
      val query = obs._2 match {
        case None => s"""insert into series values ('${id.toUpperCase}', '${obs._1.toString}', NULL)"""
        case Some(v) => s"""insert into series values ('${id.toUpperCase}', '${obs._1.toString}', $v)"""
      }
      stmt.execute(query)
    })

    conn.commit()
    stmt.closeOnCompletion()
  }

  /**
   * Load all Excel files to SQLite database
   * 
   * @param conn database connection
   * @param workdir path to downloaded Excel files
   */
  def loadAll(conn: Connection, workdir: String): Unit = {
    val links = rbnz.download(workdir)

    createSeriesDefinitionTable(conn)
    createSeriesTable(conn)

    links.flatMap(x => x._3).foreach(s => {
      println(s"${s._1} | ${s._3}...")
      val series = rbnz.importXlsxData(s._3)
      val definition = rbnz.importXlsxSeriesDefinition(s._3)

      series match {
        case Failure(e) => println("Invalid series data.")
        case Success(series_) => definition match {
          case Failure(e) => println("Invalid definition data.")
          case Success(definition_) => {
            val k1 = series_.keys.toList
            val k2 = definition_.keys.toList
            k1.foreach(k => {
              println(s"\t$k")
              if (k2.contains(k)) loadSeries(conn, series_(k), definition_(k))
            })
          }
        }
      }
    })
  }

  /**
   * Fetch series definitions.
   * 
   * @param conn database connection
   * @param id list of series IDs
   * @param groupKeyword list of keywords to search for in "group"
   * @param nameKeyword list of keywords to search for in "name"
   * @return vector containing matching series definitions
   */
  def getDefinition(
    conn: Connection, id: List[String], 
    groupKeyword: List[String], nameKeyword: List[String]
  ): Vector[(String, String, String, String, String, Option[String])] = {
    val stmt = conn.createStatement()

    val q0 = "select * from series_definition"

    val q1 = if (id.size == 0) q0 else {
      val where = id.map(x => s"'$x'").mkString(", ")
      s"$q0 where id in ($where)"
    }      

    val q2 = if (groupKeyword.size == 0) q1 else {
      val like = groupKeyword.map(kw => s""""group" like "%$kw%"""").mkString(" and ")
      s"select * from ($q1) a where $like"
    }   

    val q3 = if (nameKeyword.size == 0) q1 else {
      val like = nameKeyword.map(kw => s"""name like "%$kw%"""").mkString(" and ")
      s"select * from ($q2) b where $like"
    }

    val rs = stmt.executeQuery(q3)

    def loop(
      rs: ResultSet, accum: Vector[(String, String, String, String, String, Option[String])] 
    ): Vector[(String, String, String, String, String, Option[String])] = {
      if (!rs.next()) accum
      else {
        val toadd = (
          rs.getString(1), rs.getString(2), rs.getString(3),
          rs.getString(4), rs.getString(5), 
          rs.getString(6) match { case null => None; case x => Some(x) }
        )
        loop(rs, accum :+ toadd)
      }
    }

    loop(rs, Vector.empty)  
  }

  /**
   * Fetch series.
   * 
   * @param conn database connection
   * @param id list of series IDs
   * @return vector containing matching series
   */
  def getSeries(conn: Connection, id: List[String]): Vector[(String, String, Option[Double])] = {
    val stmt = conn.createStatement()

    val q0 = "select * from series"

    val q1 = if (id.size == 0) s"$q0 order by id, date" else {
      val where = id.map(x => s"'$x'").mkString(",")
      s"$q0 where id in ($where) order by id, date"
    }

    val rs = stmt.executeQuery(q1)

    def loop(rs: ResultSet, accum: Vector[(String, String, Option[Double])]): Vector[(String, String, Option[Double])] = {
      if (!rs.next()) accum
      else {
        val value: Option[Double] = {
          val x = rs.getDouble(3)
          if (rs.wasNull()) None else Some(x)
        }

        val row = (rs.getString(1), rs.getString(2), value)
        loop(rs, accum :+ row)
      }
    }

    loop(rs, Vector.empty)
  }

  /**
   * Fetch series definitions, return as JSON string.
   * 
   * @param conn database connection
   * @param id list of series IDs
   * @param groupKeyword list of keywords to search for in "group"
   * @param nameKeyword list of keywords to search for in "name"
   * @return JSON string
   */
  def getDefinitionJson(
    conn: Connection, id: List[String], 
    groupKeyword: List[String], nameKeyword: List[String]
  ): String = {
    val definitions = getDefinition(conn, id, groupKeyword, nameKeyword)
    val objects = definitions.map(x => {      
      val noteStr = x._6 match {
        case None => "null"
        case Some(x) => s""""$x""""
      }
      s"""{"group":"${x._1}","id":"${x._2}","name":"${x._3}",""" +
        s""""unit":"${x._4}","frequency":"${x._5}","note":$noteStr}"""
    })

    "[" + objects.mkString(",") + "]"
  }

  /**
   * Fetch series, return as JSON string.
   * 
   * @param conn database connection
   * @param id list of series IDs
   * @param groupKeyword list of keywords to search for in definition "group"
   * @param nameKeyword list of keywords to search for in definition "name"
   * @return JSON string
   */
  def getSeriesJson(
    conn: Connection, id: List[String], 
    groupKeyword: List[String], nameKeyword: List[String]
  ): String = {
    val definitions = getDefinition(conn, id, groupKeyword, nameKeyword).take(100)
    val ids = definitions.map(x => x._2).toList
    val series = getSeries(conn, ids)

    "[" + definitions.map(x => {
      val s = series.filter(y => y._1 == x._2)

      val noteStr = x._6 match {
        case None => "null"
        case Some(x) => s""""$x""""
      }

      val dates = "[" + s
        .map(x => x._2).map(d => s""""$d"""")
        .mkString(",") + "]"

      val values = "[" + s
        .map(x => x._3).map(v => v match {
          case None => "null"
          case Some(x) => s"""$x"""
        })
        .mkString(",") + "]"

      s"""{"group":"${x._1}","id":"${x._2}","name":"${x._3}",""" +
        s""""unit":"${x._4}","frequency":"${x._5}","note":$noteStr,""" + 
        s""""date":$dates,"value":$values}"""
    }).mkString(",") + "]"
  }
}