package com.datlinq.datafiniti.response

import com.typesafe.scalalogging.Logger

/**
  * Created by Tom Lous on 05/09/2017.
  * Copyright © 2017 Datlinq B.V..
  */
sealed trait DatafinitiError {
  def url: String

  def message: String

  def exception: Exception = new Exception(message)

  override def toString: String = message
}

object DatafinitiError {

  final case class WrongHttpResponseCode(code: Int, data: String, url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"HTTP error $code from $url => $data"
    optionalLogger.foreach(_.error(message))
  }

  final case class ExceededPreviewLimit(code: Int, data: String, url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"Exceeded preview limit in request ($code) from $url => $data"
    optionalLogger.foreach(_.error(message))
  }

  final case class AccessDenied(code: Int, data: String, url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"Access Denied ($code) from $url => $data"
    optionalLogger.foreach(_.error(message))
  }

  final case class APICallFailed(exceptionMessage: String, url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"Call to $url failed => $exceptionMessage"
    optionalLogger.foreach(_.error(message))
  }


  final case class NoRedirectFromDownload(url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"No valid redirect found from $url response => Redirect 303 not found"
    optionalLogger.foreach(_.error(message))
  }

  final case class UnexpectedDownloadStatus(status: String, data: String, url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"Unexpected download status $status in $url => $data"
    optionalLogger.foreach(_.error(message))
  }

  final case class NoDownloadStatus(data: String, url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"No status field in $url => $data"
    optionalLogger.foreach(_.error(message))
  }

  final case class WrappedException(t: Throwable, url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"$url: ${t.getMessage}"
    optionalLogger.foreach(_.error(message))
  }

  final case class NoDownloadLinks(data: String, url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"No download Links found from call to $url => $data"
    optionalLogger.foreach(_.error(message))
  }

}