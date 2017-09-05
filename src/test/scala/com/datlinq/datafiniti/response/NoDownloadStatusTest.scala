package com.datlinq.datafiniti.response

import com.datlinq.datafiniti.response.DatafinitiError._
import org.scalatest.FunSuite

/**
  * Created by Tom Lous on 05/09/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class NoDownloadStatusTest extends FunSuite {

  val e = NoDownloadStatus("data", "http://datlinq.com")

  test("testException") {
    assert(e.exception.getMessage.contains(e.data))
    assert(e.exception.getMessage.contains(e.url))
  }

  test("testMessage") {
    assert(e.message.contains(e.data))
  }

  test("testUrl") {
    assert(e.message.contains(e.url))
  }

  test("testToString") {
    assert(e.message === e.toString)
  }
}
