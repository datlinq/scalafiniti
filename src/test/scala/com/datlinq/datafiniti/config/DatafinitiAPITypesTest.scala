package com.datlinq.datafiniti.config

import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import org.scalatest.FunSuite

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class DatafinitiAPITypesTest extends FunSuite {

  test("toString") {
    assert(Businesses.toString === "businesses")
    assert(Products.toString === "products")
    assert(Properties.toString === "properties")
  }

  test("fromString") {
    assert(APIType.fromString("buSineSSes") === Businesses)
    assert(APIType.fromString("producTS") === Products)
    assert(APIType.fromString("propertIEs") === Properties)
    assert(APIType.fromString("custom") === CustomType("custom"))
  }

}
