package com.datlinq.datafiniti

import java.util.concurrent.ScheduledThreadPoolExecutor

import com.datlinq.datafiniti.config.DatafinitiAPIFormats._
import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import com.datlinq.datafiniti.config.DatafinitiAPIViews._
import com.netaporter.uri.dsl._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
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

  private def request[T](uri: String, followRedirect: Boolean = true)(codeCheck: Int => Boolean, successHandler: HttpResponse[String] => Either[Throwable, T]): Future[Either[Throwable, T]] = {

    Future({
      logger.debug("request:  " + safeUri(uri))
      Http(uri)
        .auth(apiToken, "")
        .option(HttpOptions.followRedirects(followRedirect))
        .asString
    })
      .map(response => {
        logger.debug(s"response from ${safeUri(uri)} => ${response.headers.mkString("|")}")
        response.code match {
          case code if codeCheck(code) => successHandler(response)
          case code => {
            val error = s"HTTP error ${response.code} from ${safeUri(uri)} => ${response.body}"
            logger.error(error)
            Left(new Exception(error))
          }
        }
      })
      .recover {
        // $COVERAGE-OFF$Not sure how to test this
        case t: Throwable => {
          logger.error(s"call from ${safeUri(uri)} failed => " + t.getMessage)
          Left(t)
        }
        // $COVERAGE-ON$
      }
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


    request(uri)(_ == 200, response => Right(parse(response.body)))
  }


  def download(apiView: APIView, query: Option[String] = None, format: APIFormat = JSON): Future[Either[Throwable, String]] = {
    val uri = buildUrl(
      apiType = apiView.apiType,
      queryParts = List(
        "view" -> apiView,
        "format" -> format,
        "q" -> query,
        "download" -> Some(true)
      ).toMap)

    /**
      * Extract redirect from 303 redirects and poll these
      *
      * @param response HttpResponse from original URL
      * @return Either an exception or a url to poll
      */
    def parseRedirect(response: HttpResponse[String]): Either[Throwable, String] = {
      (for {
        option <- response.headers.get("location")
        location <- option.headOption
      } yield location) match {
        case Some(path) => {
          Right(s"$SCHEME://$DOMAIN$path")
        }
        case None => {
          val error = s"No valid redirect found from ${safeUri(uri)} => Redirect (303)"
          logger.error(error)
          Left(new Exception(error))
        }
      }
    }

    val scheduledExecutor = new ScheduledThreadPoolExecutor(1)
    val p = Promise[List[String]]()


    //    def checkPollData(response:HttpResponse[String]):Either[Throwable, List[String]] = {
    //
    //    }
    //
    //
    //    def checkForDownloadLinks(uri: String): Future[Option[List[String]]] = Future({
    //      request(uri)(_ == 200, checkPollData).map(_ match {
    //        case Right(res) => Some(res)
    //        case Left(_) => None
    //      }
    //
    //
    //      val success = Random.nextDouble() <= 0.2
    //      println(s"Call to checkForDownloadLinks => $success")
    //      if (success) Some(List("link1", "link2")) else None
    //    })
    //
    //    def pollForDownloadLinks(uri: String): Future[List[String]] = {
    //      def scheduleDelayedPoll(p: Promise[List[String]]) = {
    //        scheduledExecutor.schedule(new Runnable {
    //          override def run() = poll(p)
    //        },
    //          10, TimeUnit.SECONDS)
    //      }
    //
    //      def poll(p: Promise[List[String]]): Unit = {
    //        checkForDownloadLinks(uri).onComplete {
    //          case s: Success[Option[List[String]]] if s.value.isDefined  => p.success(s.value.get)
    //          case f: Failure[_] => scheduleDelayedPoll(p)
    //        }
    //      }
    //
    //
    //      poll(p)
    //      p.future
    //    }


    //    val x:Int = pollForDownloadLinks

    //    val result = Await.result(pollForDownloadLinks, Duration.Inf)
    //    println(result)

    val res = for {
      result <- request(uri, followRedirect = false)(_ == 303, parseRedirect)
      pollUri <- result.right
      links <- pollForDownloadLinks(pollUri)

    } yield links


    val output = Await.result(res, Duration.Inf)


    println(output)


  }


  /**
    * Create a safe uri without API token for logging purposes
    *
    * @param uri String
    * @return String
    */
  def safeUri(uri: String): String = uri.replace(apiToken, "AAAXXXXXXXXXXXX")



}

object DatafinitiAPIv3 {

  /**
    * Creates a DatafinitiAPIv3 instance based on config apikey defined in "datafinity.apiKey"
    *
    * @param config implicitly
    * @return DatafinitiAPIv3 object
    */
  def apply()(implicit config: Config): DatafinitiAPIv3 = {
    DatafinitiAPIv3(config.getString("datafinity.apiKey"))
  }
}
