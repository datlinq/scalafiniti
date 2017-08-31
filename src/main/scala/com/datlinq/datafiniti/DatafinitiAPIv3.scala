package com.datlinq.datafiniti

import com.datlinq.datafiniti.config.DatafinitiAPIFormats._
import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import com.datlinq.datafiniti.config.DatafinitiAPIViews._
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http._


/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
case class DatafinitiAPIv3(apiToken: String) extends DatafinitiAPI with LazyLogging {

  protected val VERSION = "v3"


  /**
    * Builds the URI to be called based
    *
    * @param apiType    specifies the last part of the DatafinitiAPI path
    * @param queryParts list of key values for query string. Values set to None are omitted
    * @return url as String
    */
  private def buildUrl(apiType: APIType, queryParts: Map[String, Any]): String = {
    val uri = queryParts.foldLeft(s"$SCHEME://$apiToken:@$DOMAIN/$VERSION/data/$apiType")((uri, keyVal) => {
      uri & keyVal
    }).toString

    logger.debug("buildUrl: " + uri.replace(apiToken, "AAAXXXXXXXXXXXX"))

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
    */
  def query(apiView: APIView, query: Option[String] = None, numberOfRecords: Option[Int] = None, download: Option[Boolean] = None, format: APIFormat = JSON): Future[Either[String, String]] = {
    val uri = buildUrl(apiView.apiType, List("view" -> apiView, "format" -> format, "q" -> query, "records" -> numberOfRecords, "download" -> download).toMap)

    Future(Http(uri)
      .auth(apiToken, "")
      .option(HttpOptions.followRedirects(true))
      .asString)
      .map(response => {
        response.code match {
          case 200 => Right(response.body)
          case _ => {
            logger.error(s"HTTP error ${response.code} ${response.body}")
            Left(s"Error during call: ${response.code} ${response.body}")
          }
        }

      })


  }


}
