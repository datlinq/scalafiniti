package com.datlinq.datafiniti.response

import com.datlinq.datafiniti.response.DatafinitiError.NoResultsDownload
import org.scalatest.FunSuite

/**
  * Created by Tom Lous on 07/09/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class NoResultsDownloadTest extends FunSuite {

  val e = NoResultsDownload(101, "data", "http://datlinq.com")

  test("testException") {
    assert(e.exception.getMessage.contains(e.data))
    assert(e.exception.getMessage.contains(e.url))
    assert(e.exception.getMessage.contains(e.code))
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
