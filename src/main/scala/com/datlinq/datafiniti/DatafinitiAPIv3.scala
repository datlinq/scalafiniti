package com.datlinq.datafiniti

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

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
import scala.util.{Failure, Success}
import scalaj.http._


/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
case class DatafinitiAPIv3(apiToken: String) extends DatafinitiAPI with LazyLogging {

  protected val VERSION = "v3"
  implicit val json4sFormats: DefaultFormats.type = DefaultFormats


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


  def download(apiView: APIView, query: Option[String] = None, format: APIFormat = JSON): Unit = { //: Future[Either[Throwable, String]] = {
    val uri = buildUrl(
      apiType = apiView.apiType,
      queryParts = List(
        "view" -> apiView,
        "format" -> format,
        "q" -> query,
        "download" -> Some(true)
      ).toMap)


    // Get polling URL

    /**
      * Extract redirect from 303 redirects and poll these
      *
      * @param response HttpResponse from original URL
      * @return Either an exception or a url to poll
      */
    def extractPollingUrl(response: HttpResponse[String]): Either[Throwable, String] = {
      (for {
        option <- response.headers.get("location")
        location <- option.headOption
      } yield location) match {
        case Some(path) => {
          val redirect = s"$SCHEME://$DOMAIN$path"
          logger.error(s"Found redirect from ${safeUri(uri)} to => $redirect")
          Right(redirect)
        }
        case None => {
          val error = s"No valid redirect found from ${safeUri(uri)} response => Redirect (303)"
          logger.error(error)
          Left(new Exception(error))
        }
      }
    }

    // Poll URL

    /**
      * Poll the url every 10 seconds and when download is ready (Complete) return the url to fetch the download link
      *
      * @param pollUrl         URL to poll (redirect from original request)
      * @param pollingInterval seconds between each poll (as long as marked "Started")
      * @return Either an Exception or the download info url
      */
    def pollForDownloadInfoUrl(pollUrl: String, pollingInterval: Int = 10): Future[Either[Throwable, String]] = {

      val promiseStatus = Promise[Boolean]()
      val scheduledExecutor = new ScheduledThreadPoolExecutor(1)

      /**
        * Reads the json request
        *
        * @param response Response from the pollUrl
        * @return Either an Exception or status as ready ("Completed" => true, "Started" => false)
        */
      def checkDownloadCompleted(response: HttpResponse[String]): Either[Throwable, Boolean] = {
        (parse(response.body) \\ "status").extractOpt[String] match {
          case Some(status) if status.toUpperCase() == "COMPLETED" => Right(true)
          case Some(status) if status.toUpperCase() == "STARTED" => Right(false)
          case Some(status) => Left(new Exception(s"Unexpected download status $status => ${response.body}"))
          case None => Left(new Exception(s"No status field => ${response.body}"))
        }
      }

      /**
        * Poll the download page
        *
        * @return
        */
      def pollStatus(): Unit = {
        logger.debug(s"Do poll for status")
        request(pollUrl)(_ == 200, checkDownloadCompleted).onComplete {
          case Success(Right(true)) => {
            logger.debug(s"Download ready from ${safeUri(pollUrl)}")
            promiseStatus.success(true)
          }
          case Success(Right(false)) => {
            logger.debug(s"Download not ready yet from ${safeUri(pollUrl)}")
            scheduleDelayedPoll()
          }
          case Success(Left(l: Throwable)) => {
            logger.error(s"Check poll ${safeUri(pollUrl)} failed => ${l.getMessage}")
            promiseStatus.failure(l)
          }
          case Failure(f) => {
            logger.error(s"Polling failed to ${safeUri(pollUrl)}  => ${f.getMessage}")
            promiseStatus.failure(f)
          }
        }
      }

      def scheduleDelayedPoll() = {
        logger.debug(s"Reschedule poll in $pollingInterval seconds")
        scheduledExecutor.schedule(
          new Runnable {
            override def run() = pollStatus()
          }, pollingInterval, TimeUnit.SECONDS)
      }

      pollStatus()
      promiseStatus
        .future
        .map(_ => {
          val resultUrl = pollUrl.replace("requests", "results")
          logger.debug(s"Created resultsUrl $resultUrl from $pollUrl")
          Right(pollUrl.replace("requests", "results"))
        })
        .recover({
          case t: Throwable => {
            logger.error(s"Promise to ${safeUri(pollUrl)} failed => ${t.getMessage}")
            Left(t)
          }
        })
    }

    def extractDownloadLinks(response: HttpResponse[String]): Either[Throwable, List[String]] = {
      (parse(response.body) \\ "url").extractOpt[List[String]] match {
        case Some(urls) => {
          logger.debug(s"Found download urls in ${safeUri(response.location.getOrElse("?"))} =? $urls")
          Right(urls)
        }
        case None => {
          val errorMessage = s"No urls field => ${response.body}"
          logger.error(errorMessage)
          Left(new Exception(errorMessage))
        }
      }
    }


    val res = for {
      either1 <- request(uri, followRedirect = false)(_ == 303, extractPollingUrl)
      pollUrl <- either1.right
      //          error1 <- either1.left
      either2 <- pollForDownloadInfoUrl(pollUrl, pollingInterval = 5)
      downloadInfoUrl <- either2.right
      //          error2 <- either2.left
      either3 <- request(downloadInfoUrl)(_ == 200, extractDownloadLinks)
      downloadLinks <- either3.right
    //    error3 <- either3.left

    } yield downloadLinks


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
