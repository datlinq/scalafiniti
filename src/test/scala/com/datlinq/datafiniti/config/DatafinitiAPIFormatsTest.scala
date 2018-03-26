package com.datlinq.datafiniti.config

import com.datlinq.datafiniti.config.DatafinitiAPIFormats.{APIFormat, CSV, CustomFormat, JSON}
import org.scalatest.FunSuite

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class DatafinitiAPIFormatsTest extends FunSuite {

  test("toString") {
    assert(JSON.toString === "JSON")
    assert(CSV.toString === "CSV")
  }

  test("fromString") {
    assert(APIFormat.fromString("json") === JSON)
    assert(APIFormat.fromString("cSv") === CSV)
    assert(APIFormat.fromString("custom") === CustomFormat("custom"))
  }


}
