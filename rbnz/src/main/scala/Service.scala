package org.cmhh

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.{ Route, Directive0 }
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import scala.io.StdIn
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContextExecutor

import org.rogach.scallop._

class ServiceConf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val dbPath = opt[String](name = "database-path", required = false, default = Some("output/rbnz.sqlite"))
  verify()
}

/**
 * CORS handler... just in case.
 */
trait CORSHandler{
  private val corsResponseHeaders = List(
    headers.`Access-Control-Allow-Origin`.*,
    headers.`Access-Control-Allow-Credentials`(true),
    headers.`Access-Control-Allow-Headers`(
      "Authorization", "Content-Type", "X-Requested-With"
    )
  )
  
  private def addAccessControlHeaders: Directive0 = {
    respondWithHeaders(corsResponseHeaders)
  }
  
  private def preflightRequestHandler: Route = options {
    complete(HttpResponse(StatusCodes.OK).
      withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)))
  }
  
  def corsHandler(r: Route): Route = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }
  
  def addCORSHeaders(response: HttpResponse):HttpResponse =
    response.withHeaders(corsResponseHeaders)
}

/**
 * Data service for RBNZ data in SQLite database.
 */
object Service extends App with CORSHandler {  
  val conf = new ServiceConf(args.toIndexedSeq)

  implicit val system: ActorSystem = ActorSystem("rbnz")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  private val conn = db.copyToTempDB(conf.dbPath())

  object routes {
    val version = path("version") {
      complete(HttpEntity(ContentTypes.`application/json`, "[\"0.1.0\"]"))
    }

    val definition = path("definition") {
      parameters(
        "id".as[String].repeated, "groupKeyword".as[String].repeated, "nameKeyword".as[String].repeated
      ){ (id, groupKeyword, nameKeyword) =>
        complete(HttpEntity(
          ContentTypes.`application/json`, 
          db.getDefinitionJson(conn, id.toList, groupKeyword.toList, nameKeyword.toList)
        ))
      }
    }
    
    val series = path("series") {
      parameters(
        "id".as[String].repeated, "groupKeyword".as[String].repeated, "nameKeyword".as[String].repeated
      ){ (id, groupKeyword, nameKeyword)  =>
        complete(HttpEntity(
          ContentTypes.`application/json`, 
          db.getSeriesJson(conn, id.toList, groupKeyword.toList, nameKeyword.toList)
        ))
      }
    }
  }

  val route = pathPrefix("rbnz") { 
    corsHandler(
      get {
        routes.version ~ routes.definition ~ routes.series
      }
    )
  }

  val bindingFuture = Http().newServerAt("0.0.0.0", 9001).bindFlow(route)

  println(s"Server online at http://localhost:9001/rbnz\nPress ENTER to stop...")
  StdIn.readLine() 
  bindingFuture
    .flatMap(_.unbind()) 
    .onComplete(_ => system.terminate()) 
}