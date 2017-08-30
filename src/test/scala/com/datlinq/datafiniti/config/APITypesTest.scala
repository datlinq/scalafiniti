package com.datlinq.datafiniti.config

import com.datlinq.datafiniti.config.APITypes._
import org.scalatest.FunSuite

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class APITypesTest extends FunSuite {

  test("toString") {
    assert(Businesses.toString === "businesses")
    assert(Products.toString === "products")
  }

}
