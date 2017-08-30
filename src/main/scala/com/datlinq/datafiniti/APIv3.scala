package com.datlinq.datafiniti

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.datlinq.datafiniti.config.APIFormats._
import com.datlinq.datafiniti.config.APITypes._
import com.datlinq.datafiniti.config.APIViews._
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.ws._
import play.api.libs.ws.ahc._

import scala.concurrent.ExecutionContext.Implicits.global

//import play.api.libs.ws.JsonBodyReadables._
//import play.api.libs.ws.JsonBodyWritables._


import scala.concurrent.Future


/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
case class APIv3(apiToken: String) extends API with LazyLogging {

  protected val VERSION = "v3"

  implicit val system: ActorSystem = ActorSystem()
  system.registerOnTermination {
    System.exit(0)
  }
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  /**
    * Builds the URI to be called based
    *
    * @param apiType    specifies the last part of the API path
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
  def query(apiView: APIView, query: Option[String] = None, numberOfRecords: Option[Int] = None, download: Option[Boolean] = None, format: APIFormat = JSON): Unit = {
    val uri = buildUrl(apiView.apiType, List("view" -> apiView, "format" -> format, "q" -> query, "records" -> numberOfRecords, "download" -> download).toMap)

    val wsClient = StandaloneAhcWSClient()

    val response: Future[Unit] = wsClient
      .url(uri)
      .withAuth(apiToken, "", WSAuthScheme.BASIC)
      .withFollowRedirects(true)
      .get()
      .map { response =>
        val statusText: String = response.statusText
        val body = response.body[String]
        println(s"Got a response $statusText")
      }.andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }


    //
  }


}
