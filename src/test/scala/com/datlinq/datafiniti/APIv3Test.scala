package com.datlinq.datafiniti

import com.datlinq.datafiniti.config.APITypes._
import com.typesafe.config.ConfigFactory
import org.scalatest.{Outcome, PrivateMethodTester, fixture}

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class APIv3Test extends fixture.FunSuite with PrivateMethodTester {

  type FixtureParam = APIv3

  def withFixture(test: OneArgTest): Outcome = {
    val config = ConfigFactory.load()
    val apiKey = config.getString("apiKey")
    val apiv3 = APIv3(apiKey)
    test(apiv3)
  }


  test("private buildUrl") { apiv3 => {
    val buildUrl = PrivateMethod[String]('buildUrl)

    assert(apiv3.invokePrivate(buildUrl(Businesses, Map.empty[String, Any])) === "https://AAAXXXXXXXXXXXX:@api.datafiniti.co/v3/data/businesses")
    assert(apiv3.invokePrivate(buildUrl(Products, List("a" -> 1).toMap)) === "https://AAAXXXXXXXXXXXX:@api.datafiniti.co/v3/data/products?a=1")
    assert(apiv3.invokePrivate(buildUrl(Products, List("a" -> 1, "b" -> None, "c" -> Some(true)).toMap)) === "https://AAAXXXXXXXXXXXX:@api.datafiniti.co/v3/data/products?a=1&c=true")
    assert(apiv3.invokePrivate(buildUrl(Businesses, List("view" -> "businesses_all", "format" -> "JSON", "q" -> Some("categories:hotels"), "records" -> 1, "download" -> false).toMap)) === "https://AAAXXXXXXXXXXXX:@api.datafiniti.co/v3/data/businesses?format=JSON&q=categories:hotels&records=1&download=false&view=businesses_all")
  }
  }


}
