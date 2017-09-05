package com.datlinq.datafiniti.response

import com.typesafe.scalalogging.Logger

/**
  * Created by Tom Lous on 05/09/2017.
  * Copyright © 2017 Datlinq B.V..
  */
sealed trait DatafinitiError {
  def url: String

  def message: String
}

object DatafinitiError {

  final case class WrongHttpResponseCode(code: Int, data: String, url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"HTTP error $code from $url => $data"
    optionalLogger.foreach(_.error(message))
  }

  final case class APICallFailed(exceptionMessage: String, url: String)(implicit optionalLogger: Option[Logger] = None) extends DatafinitiError {
    val message: String = s"call to $url failed => $exceptionMessage"
    optionalLogger.foreach(_.error(message))
  }

  //
  //  final case class NoRedirectFromDownload(message: String, url: String)(implicit optionalLogger:Option[Logger]=None)  extends DatafinitiError {
  //    val message:String =s"call to $url failed => $exceptionMessage"
  //    optionalLogger.foreach(_.error(message))
  //  }
  //
  //  final case class UnexpectedDownloadStatus(status: Option[String], message: String, url: String)(implicit optionalLogger:Option[Logger]=None)  extends DatafinitiError {
  //    val message:String =s"call to $url failed => $exceptionMessage"
  //    optionalLogger.foreach(_.error(message))
  //  }
  //
  //  final case class NoDownloadStatus(message: String, url: String)(implicit optionalLogger:Option[Logger]=None)  extends DatafinitiError {
  //    val message:String =s"call to $url failed => $exceptionMessage"
  //    optionalLogger.foreach(_.error(message))
  //  }
  //
  //  final case class NoDownloadLinks(message: String, url: String)(implicit optionalLogger:Option[Logger]=None)  extends DatafinitiError {
  //    val message:String =s"call to $url failed => $exceptionMessage"
  //    optionalLogger.foreach(_.error(message))
  //  }
}