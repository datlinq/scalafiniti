package com.datlinq.datafiniti

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.datlinq.datafiniti.config.DatafinitiAPIFormats._
import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import com.datlinq.datafiniti.config.DatafinitiAPIViews._
import com.datlinq.datafiniti.response.DatafinitiTypes.{DatafinitiFuture, DatafinitiResponse}
import com.netaporter.uri.dsl._
import com.typesafe.config.Config
import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.Promise
import scala.util.{Failure, Success}

//import scala.concurrent.ExecutionContext.Implicits.global
import cats.data._
import cats.implicits._
import com.datlinq.datafiniti.response.DatafinitiError

import scala.concurrent.{ExecutionContext, Future}
import scalaj.http._


/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
case class DatafinitiAPIv3(apiToken: String) extends DatafinitiAPI with LazyLogging {

  protected val VERSION = "v3"
  implicit val json4sFormats: DefaultFormats.type = DefaultFormats
  implicit val loggerOpt: Option[Logger] = Some(logger)


  /**
    * Builds the URI to be called based
    *
    * @param apiType    specifies the last part of the DatafinitiAPI path
    * @param queryParts list of key values for query string. Values set to None are omitted
    * @return url as String
    */
  private def buildUrl(apiType: APIType, queryParts: Map[String, Any]): String = {
    val url = queryParts.foldLeft(s"$SCHEME://$DOMAIN/$VERSION/data/$apiType")((uri, keyVal) => {
      uri & keyVal
    }).toString

    logger.debug("buildUrl: " + safeUrl(url))

    url
  }

  /**
    * Make call to url with Datafiniti authentication
    *
    * @param url            url to call
    * @param followRedirect boolean for choosing to follow the call in case of 30x
    * @param codeCheck      method that validates the call based on httpresponse code, default 200 ok
    * @param successHandler convert the url & responsebody to appropriate value of type T wrapped in Either[DatafinitiError, T]
    * @param ec             Execution context for futures
    * @tparam T Type of return parameter returned by successHandler
    * @return EitherT[Future, DatafinitiError, T]
    */
  private def request[T](url: String, followRedirect: Boolean = true)(successHandler: (String, HttpResponse[String]) => DatafinitiResponse[T], codeCheck: Int => Boolean = _ == 200)(implicit ec: ExecutionContext): DatafinitiFuture[T] = {
    EitherT(
      Future({
        logger.debug(s"request: ${safeUrl(url)}, followRedirect: $followRedirect")
        Http(url)
          .auth(apiToken, "")
          .option(HttpOptions.followRedirects(followRedirect))
          .asString
      })
        .map(response => {
          logger.debug(s"response from ${safeUrl(url)} => ${response.headers.map(kv => kv._1 + ": " + kv._2.mkString(",")).mkString(" | ")}")
          response.code match {
            case code if codeCheck(code) => successHandler(url, response)
            case code => Left(DatafinitiError.WrongHttpResponseCode(response.code, response.body, safeUrl(url)))
          }
        })
        .recover {
          // $COVERAGE-OFF$Not sure how to test this
          case t: Throwable => Left(DatafinitiError.APICallFailed(t.getMessage, safeUrl(url)))
          // $COVERAGE-ON$
        }
    )
  }

  /**
    * Queries datafinity
    *
    * @param apiView         The view of the data
    * @param query           Filter of fields
    * @param numberOfRecords how many records to return in the its response.
    * @param download        initiate a download request or not
    * @param format          which data format you want to see. You can set it to JSON or CSV.
    * @param ec              Execution context for futures
    * @return EitherT[Future, DatafinitiError, JValue]
    */
  def query(apiView: APIView, query: Option[String] = None, numberOfRecords: Option[Int] = None, download: Option[Boolean] = None, format: APIFormat = JSON)(implicit ec: ExecutionContext): DatafinitiFuture[JValue] = {
    val url = buildUrl(
      apiType = apiView.apiType,
      queryParts = List(
        "view" -> apiView,
        "format" -> format,
        "q" -> query,
        "records" -> numberOfRecords,
        "download" -> download
      ).toMap)


    request(url)((url, response) => Right(parse(response.body)))
  }


  /**
    * Do query but returns urls for the download of the resulting dataset
    *
    * @param apiView The view of the data
    * @param query   Filter of fields
    * @param format  which data format you want to see. You can set it to JSON or CSV.
    * @param ec      Execution context for futures
    * @return EitherT[Future, DatafinitiError, List[String]  with the list containing the download links
    */
  def downloadLinks(apiView: APIView, query: Option[String] = None, format: APIFormat = JSON)(implicit ec: ExecutionContext): DatafinitiFuture[List[String]] = {
    val requestDownloadUrl = buildUrl(
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
      * @param url      requestDownloadUrl
      * @param response HttpResponse from original URL
      * @return Either an DatafinitiError or a url to poll
      */
    def extractPollingUrl(url: String, response: HttpResponse[String]): DatafinitiResponse[String] = {
      (for {
        option <- response.headers.get("location")
        location <- option.headOption
      } yield location) match {
        case Some(path) => {
          val redirect = s"$SCHEME://$DOMAIN$path"
          logger.debug(s"Found redirect from ${safeUrl(url)} to => $redirect")
          Right(redirect)
        }
        case None => Left(DatafinitiError.NoRedirectFromDownload(safeUrl(url)))
      }
    }

    // Poll URL

    /**
      * Poll the url every 10 seconds and when download is ready (Complete) return the url to fetch the download link
      *
      * @param pollUrl         URL to poll (redirect from original request)
      * @param pollingInterval seconds between each poll (as long as marked "Started")
      * @param ec              Execution context for futures
      * @return EitherT[Future, DatafinitiError, String] with String being the dowloadInfoUrl
      */
    def pollForDownloadInfoUrl(pollUrl: String, pollingInterval: Int = 10)(implicit ec: ExecutionContext): DatafinitiFuture[String] = {

      val promiseStatus = Promise[Boolean]()
      val scheduledExecutor = new ScheduledThreadPoolExecutor(1)

      /**
        * Reads the json request
        *
        * @param url      the pollUrl
        * @param response Response from the pollUrl
        * @return Either an DatafinitiError or status as ready ("Completed" => true, "Started" => false)
        */
      def checkDownloadCompleted(url: String, response: HttpResponse[String]): DatafinitiResponse[Boolean] = {
        (parse(response.body) \\ "status").extractOpt[String] match {
          case Some(status) if status.toUpperCase() == "COMPLETED" => Right(true)
          case Some(status) if status.toUpperCase() == "STARTED" => Right(false)
          case Some(status) => Left(DatafinitiError.UnexpectedDownloadStatus(status, response.body, safeUrl(url)))
          case None => Left(DatafinitiError.NoDownloadStatus(response.body, safeUrl(url)))
        }
      }

      /**
        * Poll the download page
        * Updates the promiseStatus
        *
        * @param ec Execution context for futures
        */
      def pollStatus()(implicit ec: ExecutionContext): Unit = {
        logger.debug(s"Do poll for status")
        request(pollUrl)(checkDownloadCompleted).value.onComplete {
          case Success(Right(true)) => {
            logger.debug(s"Download ready from ${safeUrl(pollUrl)}")
            promiseStatus.success(true)
          }
          case Success(Right(false)) => {
            logger.debug(s"Download not ready yet from ${safeUrl(pollUrl)}")
            scheduleDelayedPoll()
          }
          case Success(Left(error: DatafinitiError)) => {
            logger.error(s"Check poll ${error.url} failed => $error")
            promiseStatus.failure(error.exception)
          }
          case Failure(f) => {
            logger.error(s"Polling failed to ${safeUrl(pollUrl)}  => ${f.getMessage}")
            promiseStatus.failure(f)
          }
        }
      }

      /**
        * Schedules a call to the pollStatus method in pollingInterval seconds
        *
        * @param ec Execution context for futures
        */
      def scheduleDelayedPoll()(implicit ec: ExecutionContext) = {
        logger.debug(s"Reschedule poll in $pollingInterval seconds")
        scheduledExecutor.schedule(
          new Runnable {
            override def run() = pollStatus()
          }, pollingInterval, TimeUnit.SECONDS)
      }

      // Trigger first poll check
      pollStatus()


      // Wrap the future of the promise in EitherT
      EitherT(
        promiseStatus
          .future
          .map(_ => {
            val resultUrl = pollUrl.replace("requests", "results")
            logger.debug(s"Created resultsUrl $resultUrl from $pollUrl")
            Right(resultUrl)
          })
          .recover({
            case t: Throwable => Left(DatafinitiError.WrappedException(t, pollUrl))
          })
      )
    }


    // Extract download links
    /**
      * Extracts the download links from JSON
      *
      * @param url      downloadInfoUrl where to find downloads
      * @param response Json containing urls
      * @return Either a DatafinitiError or a List of download URL's
      */
    def extractDownloadLinks(url: String, response: HttpResponse[String]): DatafinitiResponse[List[String]] = {
      val json = parse(response.body) \ "url"
      val urls = json.children.flatMap(_.extractOpt[String])

      if (urls.nonEmpty) {
        logger.debug(s"Found download urls in ${safeUrl(url)} => $urls")
        Right(urls)
      }
      else {
        Left(DatafinitiError.NoDownloadLinks(response.body, safeUrl(url)))
      }
    }


    // Execute all the download steps
    for {
      pollUrl <- request(requestDownloadUrl, followRedirect = false)(extractPollingUrl, _ == 303)
      downloadInfoUrl <- pollForDownloadInfoUrl(pollUrl, pollingInterval = 5)
      downloadLinks <- request(downloadInfoUrl)(extractDownloadLinks)
    } yield downloadLinks

  }


  /**
    * Create a safe url without API token for logging purposes
    *
    * @param url String
    * @return String
    */
  def safeUrl(url: String): String = url.replace(apiToken, "AAAXXXXXXXXXXXX")


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
