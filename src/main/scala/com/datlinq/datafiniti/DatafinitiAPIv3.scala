package com.datlinq.datafiniti

import com.datlinq.datafiniti.config.DatafinitiAPIFormats._
import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import com.datlinq.datafiniti.config.DatafinitiAPIViews._
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scalaj.http._


/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
case class DatafinitiAPIv3(apiToken: String) extends DatafinitiAPI with LazyLogging {

  protected val VERSION = "v3"
  implicit val json4sFormats = DefaultFormats


  /**
    * Builds the URI to be called based
    *
    * @param apiType    specifies the last part of the DatafinitiAPI path
    * @param queryParts list of key values for query string. Values set to None are omitted
    * @return url as String
    */
  private def buildUrl(apiType: APIType, queryParts: Map[String, Any]): String = {
    val uri = queryParts.foldLeft(s"$SCHEME://$DOMAIN/$VERSION/data/$apiType")((uri, keyVal) => {
      uri & keyVal
    }).toString

    logger.debug("buildUrl: " + safeUri(uri))

    uri
  }


  /**
    * Queries datafinity
    *
    * @param apiView         The view of the data
    * @param query           Filter of fields
    * @param numberOfRecords how many records to return in the its response.
    * @param download        initiate a download request or not
    * @param format          which data format you want to see. You can set it to JSON or CSV.
    * @return Future of the body of the query as String
    */
  def query(apiView: APIView, query: Option[String] = None, numberOfRecords: Option[Int] = None, download: Option[Boolean] = None, format: APIFormat = JSON): Future[Either[Throwable, JValue]] = {
    val uri = buildUrl(
      apiType = apiView.apiType,
      queryParts = List(
        "view" -> apiView,
        "format" -> format,
        "q" -> query,
        "records" -> numberOfRecords,
        "download" -> download
      ).toMap)


    Future({
      logger.debug("query:  " + safeUri(uri))
      Http(uri)
        .auth(apiToken, "")
        .option(HttpOptions.followRedirects(true))
        .asString
    })
      .map(response => {
        logger.debug(s"response from ${safeUri(uri)} => ${response.headers}")
        response.code match {
          case 200 => {
            Right(parse(response.body))
          }
          case c => {
            val error = s"HTTP error ${response.code} from ${safeUri(uri)} => ${response.body}"
            logger.error(error)
            val errorMessage = Try(s"HTTP $c:" + (parse(response.body) \ "error").extract[String]).getOrElse(error)
            Left(new Exception(errorMessage))
          }
        }
      }).recover {
      // $COVERAGE-OFF$Not sure how to test this
      case t: Throwable => {
        logger.error(s"call from ${safeUri(uri)} failed => " + t.getMessage)
        Left(t)
      }
      // $COVERAGE-ON$
    }
  }


  /**
    * Create a safe uri without API token for logging purposes
    *
    * @param uri String
    * @return String
    */
  def safeUri(uri: String): String = uri.replace(apiToken, "AAAXXXXXXXXXXXX")



}
