package com.datlinq.datafiniti.config

import com.datlinq.datafiniti.config.APIFormats.{CSV, JSON}
import org.scalatest.FunSuite

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class APIFormatsTest extends FunSuite {

  test("toString") {
    assert(JSON.toString === "JSON")
    assert(CSV.toString === "CSV")
  }


}
