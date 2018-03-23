package com.datlinq.datafiniti

import java.io.OutputStream
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import cats.data._
import cats.implicits._
import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import com.datlinq.datafiniti.request.SearchRequest._
import com.datlinq.datafiniti.response.DatafinitiError
import com.datlinq.datafiniti.response.DatafinitiTypes.{DatafinitiFuture, DatafinitiResponse}
import com.netaporter.uri.dsl._
import com.typesafe.config.Config
import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._

import scala.concurrent.{Promise, _}
import scala.io.Codec
import scala.util.{Failure, Success, Try}
import scalaj.http._


/**
  * Created by Tom Lous on 31/01/2018.
  */

/**
  * Create new DatafinitiAPIv4 Object
  *
  * @param email              account email supplied by datafinity
  * @param password           password email supplied by datafinity
  * @param httpTimeoutSeconds default, 3600 seconds
  */
case class DatafinitiAPIv4(email: String, password: String, httpTimeoutSeconds: Int = 3600)(implicit ec: ExecutionContext) extends DatafinitiAPI with LazyLogging {

  import scala.concurrent.duration._

  protected val VERSION = "v4"
  protected val API_URL = s"$SCHEME://$DOMAIN/$VERSION"


  implicit val loggerOpt: Option[Logger] = Some(logger)
  implicit val json4sFormats = DefaultFormats + new SearchRequestSerializerV4()


  /**
    * Private class that handles the auth JWT token retrieval and refreshing in all subsequent calls
    *
    * @param expiresDefault if no expiretimeout is passed, use default num of seconds until token has to be refreshed
    * @param ec             Execution context for futures
    */
  private case class AuthAccessToken(expiresDefault: Int = 120)(implicit ec: ExecutionContext) {
    val authUrl = s"$API_URL/auth" // auth endpoint for NIC

    /**
      * Request body data structure of auth call
      *
      * @param email    username
      * @param password password (PLAINTEXT!!!)
      */
    private case class AuthAccessTokenFetcherBody(email: String, password: String)

    /**
      * Private class that contains the token and updates local var with new token after timeut expires
      *
      * @param token
      */
    private case class AccessToken(token: String) {
      val deadline: Deadline = expiresDefault seconds fromNow
      Future(Await.ready(Promise().future, deadline.timeLeft)) onComplete (_ => {
        currentAccessToken = AccessToken(fetchToken)
      })
    }


    /**
      * Actual call to request the JWT access token from auth endpoint
      *
      * @param ec Execution context for futures
      * @return the JWT as String or empty String incase something went wrong
      */
    private def fetchToken(implicit ec: ExecutionContext): String = {
      Await.result(post(authUrl, Some(AuthAccessTokenFetcherBody(email, password)), accessToken = None)((_, response) =>
        Right((parse(response.body) \ "token").extract[String])
      ).value, httpTimeoutSeconds seconds) match {
        case Right(token) =>
          logger.debug(s"JWT token $token")
          token
        case Left(t) =>
          logger.error(t.toString)
          ""
      }
    }

    /**
      * Create new MUTABLE accessToken
      */
    @volatile private var currentAccessToken = AccessToken(fetchToken)

    /**
      * Get AccessToken object
      *
      * @return
      */
    def accessToken: String = currentAccessToken.token

    /**
      * Currnt accessToken as String
      *
      * @return
      */
    override def toString: String = accessToken
  }


  // Generate a default reference to the AccessToken Fecther for use in all methods
  private lazy val currentAccessToken = AuthAccessToken()


  /**
    * Builds the URI to be called based
    *
    * @param apiType    specifies the last part of the DatafinitiAPI path
    * @param queryParts list of key values for query string. Values set to None are omitted
    * @return url as String
    */
  private def buildUrl(apiType: APIType, queryParts: Map[String, Any] = Map.empty): String = {
    val url = queryParts.foldLeft(s"$API_URL/$apiType/search")((uri, keyVal) => {
      uri & keyVal
    }).toString

    logger.debug("buildUrl: " + url)

    url
  }


  /**
    * Make GET call to url with Datafiniti authentication
    *
    * @param url            url to call
    * @param followRedirect boolean for choosing to follow the call in case of 30x
    * @param codeCheck      method that validates the call based on httpresponse code, default 200 ok
    * @param successHandler convert the url & responsebody to appropriate value of type T wrapped in Either[DatafinitiError, T]
    * @param ec             Execution context for futures
    * @tparam T Type of return parameter returned by successHandler
    * @return EitherT[Future, DatafinitiError, T]
    */
  private def get[T](url: String, followRedirect: Boolean = true, accessToken: Option[AuthAccessToken] = Some(currentAccessToken))(successHandler: (String, HttpResponse[String]) => DatafinitiResponse[T], codeCheck: Int => Boolean = _ == 200)(implicit ec: ExecutionContext): DatafinitiFuture[T] = {
    val http = Http(url)
          .timeout(httpTimeoutSeconds * 1000, httpTimeoutSeconds * 10000)
          .option(HttpOptions.followRedirects(followRedirect))

    call[T](url, s"get: $url, followRedirect: $followRedirect", optionallyAddAuthorizationBearer(http, accessToken))(successHandler, codeCheck, ec)
  }


  /**
    * Make POST call to url with Datafiniti authentication
    *
    * @param url            url to call
    * @param data           optional data to encode in body
    * @param followRedirect boolean for choosing to follow the call in case of 30x
    * @param codeCheck      method that validates the call based on httpresponse code, default 200 ok
    * @param successHandler convert the url & response body to appropriate value of type T wrapped in Either[DatafinitiError, T]
    * @param ec             Execution context for futures
    * @tparam T Type of return parameter returned by successHandler
    * @tparam D Type of data passed to post
    * @return EitherT[Future, DatafinitiError, T]
    */
  private def post[T, D <: AnyRef : Manifest](url: String, data: Option[D], followRedirect: Boolean = true, accessToken: Option[AuthAccessToken] = Some(currentAccessToken))(successHandler: (String, HttpResponse[String]) => DatafinitiResponse[T], codeCheck: Int => Boolean = _ == 200)(implicit ec: ExecutionContext): DatafinitiFuture[T] = {
    val http = Http(url)
      .timeout(httpTimeoutSeconds * 1000, httpTimeoutSeconds * 10000)
      .option(HttpOptions.followRedirects(followRedirect))
      .postData(data.map(d => {
        val json = write(d)
        logger.debug(s"data: $json")
        json
      }).getOrElse(""))

    call[T](url, s"post: $url, data: $data, followRedirect: $followRedirect", optionallyAddAuthorizationBearer(http, accessToken))(successHandler, codeCheck, ec)
  }

  /**
    * Perform a call to Datafinity endpoint
    *
    * @param url            url to call
    * @param logString      log message to output when calling
    * @param http           The HttpRequest object
    * @param successHandler convert the url & response body to appropriate value of type T wrapped in Either[DatafinitiError, T]
    * @param codeCheck      method that validates the call based on httpresponse code, default 200 ok
    * @param ec             Execution context for futures
    * @tparam T Type of return parameter returned by successHandler
    * @return EitherT[Future, DatafinitiError, T]
    */
  private def call[T](url: String, logString: String, http: HttpRequest)(implicit successHandler: (String, HttpResponse[String]) => DatafinitiResponse[T], codeCheck: Int => Boolean = _ == 200, ec: ExecutionContext): DatafinitiFuture[T] = {
    EitherT(
      Future({
        logger.debug(logString)
        http.asString
      })
        .map(response => responseHandler(url, response))
        .recover(recoverHandler(url))
    )
  }

  /**
    * Handle responses from HttpRequest call
    *
    * @param url            url that was called
    * @param response       HttpResponse object
    * @param successHandler convert the url & response body to appropriate value of type T wrapped in Either[DatafinitiError, T]
    * @param codeCheck      method that validates the call based on httpresponse code, default 200 ok
    * @tparam T Type of return parameter returned by successHandler
    * @return EitherT[Future, DatafinitiError, T]
    */
  private def responseHandler[T](url: String, response: HttpResponse[String])(implicit successHandler: (String, HttpResponse[String]) => DatafinitiResponse[T], codeCheck: Int => Boolean = _ == 200): DatafinitiResponse[T] = {
    logger.debug(s"response from $url => ${response.headers.map(kv => kv._1 + ": " + kv._2.mkString(",")).mkString(" | ")}")
    response.code match {
      case code if codeCheck(code) => successHandler(url, response)
      case code if code == 401 => Left(DatafinitiError.AccessDenied(code, response.body, url))
      case code if code == 403 && response.body.contains("exceeds preview record limit") => Left(DatafinitiError.ExceededPreviewLimit(code, response.body, url))
      case code if code == 400 && response.body.contains("numRequested cannot be <= 0") => Left(DatafinitiError.NoResultsDownload(code, response.body, url))
      case code => Left(DatafinitiError.WrongHttpResponseCode(code, response.body, url))
    }
  }

  /**
    * Partial function to handle Errors in a correct way
    *
    * @param url url that was called
    * @tparam T Type of return parameter returned by successHandler
    * @return PartialFunction[Throwable, DatafinitiResponse[T]]
    **/
  private def recoverHandler[T](url: String): PartialFunction[Throwable, DatafinitiResponse[T]] = {
    // $COVERAGE-OFF$Not sure how to test this
    case t: Throwable => Left(DatafinitiError.APICallFailed(t.getMessage, url))
    // $COVERAGE-ON$
  }

  /**
    * Optionally supplement header with bearer token if it was specified
    *
    * @param http            HttpRequest
    * @param authAccessToken optional AuthToken
    * @return HttpRequest
    */
  private def optionallyAddAuthorizationBearer(http: HttpRequest, authAccessToken: Option[AuthAccessToken] = None): HttpRequest = {
    authAccessToken match {
      case Some(accessToken) => http.header("Authorization", s"Bearer $accessToken")
      case None => http
    }
  }


  /**
    * Queries datafinity, searching
    *
    * @param searchRequest   The Search Query
    * @return EitherT[Future, DatafinitiError, JValue]
    */
  def search(searchRequest: SearchRequestV4)(implicit ec: ExecutionContext): DatafinitiFuture[JValue] = {
    val url = buildUrl(apiType = searchRequest.view_name.apiType)
    post(url, Some(searchRequest))((_, response) => Right(parse(response.body)))
  }


  /**
    * Do query but returns urls for the download of the resulting dataset
    *
    * @param searchRequest   The Search Query
    * @param ec              Execution context for futures
    * @return EitherT[Future, DatafinitiError, List[String]  with the list containing the download links
    */
  def downloadLinks(searchRequest: SearchRequestV4)(implicit ec: ExecutionContext): DatafinitiFuture[List[String]] = {
    val augmentedSearchRequest = searchRequest.copy(download = true)

    val requestDownloadUrl = buildUrl(apiType = augmentedSearchRequest.view_name.apiType)

    val url = requestDownloadUrl


    // Get polling URL

    /*
     * Extract redirect from 303 redirects and poll these
     *
     * @param url      requestDownloadUrl
     * @param response HttpResponse from original URL
     * @return Either an DatafinitiError or a url to poll
     */
    def extractPollingUrl(url: String, response: HttpResponse[String]): DatafinitiResponse[String] = {
      (for {
        downloadId <- (parse(response.body) \ "id").extractOpt[Int]
      } yield downloadId) match {
        case Some(id) =>
          val redirect = s"$API_URL/downloads/$id"
          logger.debug(s"Found redirect from ${url} to => $redirect")
          Right(redirect)
        case None => Left(DatafinitiError.NoRedirectFromDownload(url))
      }
    }

    // Poll URL

    /*
     * Poll the url every 10 seconds and when download is ready (Complete) return the url to fetch the download link
     *
     * @param pollUrl         URL to poll (redirect from original request)
     * @param pollingInterval seconds between each poll (as long as marked "Started")
     * @param ec              Execution context for futures
     * @return EitherT[Future, DatafinitiError, String] with String being the dowloadInfoUrl
     */
    def pollForDownloadInfoUrl(pollUrl: String, pollingInterval: Int)(implicit ec: ExecutionContext): DatafinitiFuture[String] = {

      val promiseStatus = Promise[Boolean]()
      val scheduledExecutor = new ScheduledThreadPoolExecutor(1)

      /*
       * Reads the json request
       *
       * @param url      the pollUrl
       * @param response Response from the pollUrl
       * @return Either an DatafinitiError or status as ready ("Completed" => true, "Started" => false)
       */
      def checkDownloadCompleted(url: String, response: HttpResponse[String]): DatafinitiResponse[(Boolean, Option[Double])] = {
        val json = parse(response.body)
        val status = (json \\ "status").extractOpt[String]
        val total = (json \\ "numRequested").extractOpt[Double]
        val downloaded = (json \\ "numDownloaded").extractOpt[Double]

        val percentage: Option[Double] = (total, downloaded) match {
          case (Some(t), Some(d)) => Some(d / t * 100.0)
          case _ => None
        }

        status match {
          case Some(s) if s.toUpperCase() == "COMPLETED" => Right((true, percentage))
          case Some(s) if s.toUpperCase() == "STARTED" => Right((false, percentage))
          case Some(s) => Left(DatafinitiError.UnexpectedDownloadStatus(s, response.body, url))
          case None => Left(DatafinitiError.NoDownloadStatus(response.body, url))
        }
      }

      /*
       * Poll the download page
       * Updates the promiseStatus
       *
       * @param ec Execution context for futures
       */
      def pollStatus()(implicit ec: ExecutionContext): Unit = {
        logger.debug(s"Do poll for status")
        get(pollUrl)(checkDownloadCompleted).value.onComplete {
          case Success(Right((true, _))) =>
            logger.debug(s"Download ready from $pollUrl")
            promiseStatus.success(true)
          case Success(Right((false, percentageDone))) =>
            logger.debug(percentageDone.map(p => f"$p%.2f%% ").getOrElse("") + s"Download not ready yet from $pollUrl")
            scheduleDelayedPoll()
          case Success(Left(error: DatafinitiError)) =>
            logger.error(s"Check poll ${error.url} failed => $error")
            promiseStatus.failure(error.exception)
          case Failure(f) =>
            logger.error(s"Polling failed to $pollUrl  => ${f.getMessage}")
            promiseStatus.failure(f)

        }
      }


      /*
       * Schedules a call to the pollStatus method in pollingInterval seconds
       *
       * @param ec Execution context for futures
       */
      def scheduleDelayedPoll()(implicit ec: ExecutionContext) = {
        // $COVERAGE-OFF$No idea how to test
        logger.debug(s"Reschedule poll in $pollingInterval seconds")
        scheduledExecutor.schedule(
          new Runnable {
            override def run(): Unit = pollStatus()
          }, pollingInterval, TimeUnit.SECONDS)
        // $COVERAGE-ON$
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
    /*
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
        logger.debug(s"Found download urls in ${url} => $urls")
        Right(urls)
      }
      else {
        Left(DatafinitiError.NoDownloadLinks(response.body, url))
      }
    }


    // Execute all the download steps
    for {
      pollUrl <- post(url, Some(augmentedSearchRequest))(extractPollingUrl, _ == 202)
      downloadInfoUrl <- pollForDownloadInfoUrl(pollUrl, pollingInterval = 5)
      downloadLinks <- get(downloadInfoUrl)(extractDownloadLinks)
    } yield downloadLinks

  }


  /**
    * Do query and stream the output to outputStream. !!Lines will be out of order!!
    *
    * @param searchRequest   The Search Query
    * @param outputStream    To which the lines are appended
    * @param sequential      If true, downlods will trigger after eacht other, not in parallel
    * @param ec              Execution context for futures
    * @return EitherT[Future, DatafinitiError, Int]  where the int is the number of lines imported
    */
  def download(searchRequest: SearchRequestV4, sequential: Boolean = false)(outputStream: OutputStream)(implicit ec: ExecutionContext): DatafinitiFuture[Int] = {
    val eitherLinksOrError = downloadLinks(searchRequest)
    var counter = 0

    /*
      * Read data from url and append to outputstream
      * @param url to download
      * @return number of lines processed (Int)
      */
    def dataToStream(url: String): Int = {
      logger.debug(s"Download from $url")
      implicit val codec: Codec = Codec.UTF8 // @todo check if this works
      Http(url).timeout(httpTimeoutSeconds * 1000, httpTimeoutSeconds * 1000).execute(
        inputStream => Try({
          var markedFailed = false
          val markedFailedContent: StringBuilder = new StringBuilder("Not valid JSON/CSV : \n")
          val num = scala.io.Source.fromInputStream(inputStream).getLines().map(line => {
            if (line.head == '<') {
              markedFailed = true
            }
            if (!markedFailed) {
              blocking {
                counter = counter + 1
                if (counter % 1000 == 0) logger.debug(s"Streamed $counter lines to outputstream")
                outputStream.write((line + "\n").getBytes)
              }
              1
            } else {
              markedFailedContent.append(line)
              0
            }
          }).sum

          if (markedFailed) {
            throw new Exception("Not valid JSON/CSV :" + markedFailedContent.toString())
          }

          num
        }) match {
          case Success(x) => x
          case Failure(t) =>
            logger.error(s"Failed download from $url: ${t.getMessage}")
            0
        }
      ).body
    }

    if (sequential) {
      val total = eitherLinksOrError.map(_.map(url => dataToStream(url)).sum)
      logger.debug(s"Streamed total of $total lines to outputstream")
      total

    } else {
      eitherLinksOrError.map(_.map(url => {
        Future {
          dataToStream(url)
        }
      })).map(listFutures => {
        logger.debug(s"Defined ${listFutures.size} futures to sequence")
        val fileFutures = Future.sequence(listFutures).map(_.sum)
        val total = Await.result(fileFutures, Duration.Inf)
        logger.debug(s"Streamed total of $total lines to outputstream")
        total
      })
    }

  }


  /**
    * Fetch the current user information as Json (JValue)
    *
    * @param specificField optionally parse a specific field out of json
    * @param ec            Execution context for futures
    * @return EitherT[Future, DatafinitiError, JValue] with json or parsed json user info
    */
  def userInfo(specificField: Option[String] = None)(implicit ec: ExecutionContext): DatafinitiFuture[JValue] = {
    val url = s"$API_URL/users/"


    def userInfoResponse(body: String) = {
      val json = parse(body)

      specificField match {
        case Some(field) => json \ field
        case None => json
      }
    }

    get(url)((_, response) => Right(userInfoResponse(response.body)))
  }


  /**
    * Calls the userInfo method with a field and extracts the core value (T)
    *
    * @param field name of the extracted field
    * @param ec    Execution context for futures
    * @tparam T type to be extracted from json
    * @return EitherT[Future, DatafinitiError, Option[T] with T wrapped in an option baed on the success of the json extraction
    */
  def userInfoField[T: Manifest](field: String)(implicit ec: ExecutionContext): DatafinitiFuture[Option[T]] = {
    userInfo(Some(field)).map(
      _.extractOpt[T])
  }


}

object DatafinitiAPIv4 {

  /**
    * Creates a DatafinitiAPIv4 instance based on config email & password defined in "datafinity.email" & "datafinity.password"
    *
    * @param httpTimeoutSeconds default, 3600 seconds
    * @param config             implicitly
    * @return DatafinitiAPIv4 object
    */
  def apply(httpTimeoutSeconds: Int)(implicit config: Config, ec: ExecutionContext): DatafinitiAPIv4 = {
    DatafinitiAPIv4(config.getString("datafinity.email"), config.getString("datafinity.password"), httpTimeoutSeconds)
  }

  /**
    * Creates a DatafinitiAPIv4 instance based on config email & password defined in "datafinity.email" & "datafinity.password"
    *
    * @param config implicitly
    * @return DatafinitiAPIv4 object
    */
  def apply()(implicit config: Config, ec: ExecutionContext): DatafinitiAPIv4 = {
    DatafinitiAPIv4(config.getString("datafinity.email"), config.getString("datafinity.password"))
  }
}
