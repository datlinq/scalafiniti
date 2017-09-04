package com.datlinq.datafiniti

import com.datlinq.datafiniti.config.DatafinitiAPIFormats.{CSV, JSON}
import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import com.datlinq.datafiniti.config.DatafinitiAPIViews.{BusinessesAllBasic, ProductsAll}
import com.typesafe.config.{Config, ConfigFactory}
import org.json4s._
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._


/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class DatafinitiAPIv3Test extends fixture.FunSuite with PrivateMethodTester {

  type FixtureParam = DatafinitiAPIv3
  implicit val json4sFormats = DefaultFormats

  def withFixture(test: OneArgTest): Outcome = {
    implicit val config: Config = ConfigFactory.load()
    val apiv3 = DatafinitiAPIv3()
    test(apiv3)
  }



  test("private buildUrl") { apiv3 => {
    val buildUrl = PrivateMethod[String]('buildUrl)
    val token = ""

    def invoke(apiType: APIType, queryParts: Map[String, Any]): String = {
      apiv3.invokePrivate(buildUrl(apiType, queryParts)).replace(apiv3.apiToken + ":@", token)
    }

    assert(invoke(Businesses, Map.empty[String, Any]) === s"https://api.datafiniti.co/v3/data/businesses")
    assert(invoke(Products, List("a" -> 1).toMap) === s"https://api.datafiniti.co/v3/data/products?a=1")
    assert(invoke(Products, List("a" -> 1, "b" -> None, "c" -> Some(true)).toMap) === s"https://api.datafiniti.co/v3/data/products?a=1&c=true")
    assert(invoke(Businesses, List("view" -> "businesses_all", "format" -> "JSON", "q" -> Some("categories:hotels"), "records" -> 1, "download" -> false).toMap) === s"https://api.datafiniti.co/v3/data/businesses?format=JSON&q=categories:hotels&records=1&download=false&view=businesses_all")
  }
  }


  ignore("query") { apiv3 => {
    val compositeFuture = {
      for {
        future1 <- apiv3.query(BusinessesAllBasic, Some("categories:hotels"), Some(1), Some(false), JSON)
        future2 <- apiv3.query(ProductsAll, Some("non-existing"), Some(1), Some(false), JSON)
        future3 <- apiv3.query(BusinessesAllBasic, Some("categories:hotels"), Some(1), Some(false), CSV)
      } yield (future1, future2, future3)
    }

    val outputs = Await.result(compositeFuture, Duration.Inf)

    assert(outputs._1.isInstanceOf[Right[Throwable, JValue]])
    assert(outputs._2.isInstanceOf[Left[Throwable, JValue]])
    assert(outputs._3.isInstanceOf[Right[Throwable, JValue]])
    assert(outputs._1.map(json => (json \ "estimated total").extract[Int]).getOrElse(0) > 10000)
    assert(outputs._2.left.get.getMessage.contains("user does not have access to this view"))
  }
  }


  test("download") { apiv3 => {
    val compositeFuture = {
      for {
        future1 <- apiv3.download(BusinessesAllBasic, Some("""categories:hotels AND city:"Den Helder""""), JSON)
      //        future2 <- apiv3.query(ProductsAll, Some("non-existing"), Some(1), Some(false), JSON)
      //        future3 <- apiv3.query(BusinessesAllBasic, Some("categories:hotels"), Some(1), Some(false), CSV)
      } yield (future1)
    }

    val outputs = Await.result(compositeFuture, Duration.Inf)

    //    assert(outputs._1.isInstanceOf[Right[Throwable, JValue]])
    //    assert(outputs._2.isInstanceOf[Left[Throwable, JValue]])
    //    assert(outputs._3.isInstanceOf[Right[Throwable, JValue]])
    //    assert(outputs._1.map(json => (json \ "estimated total").extract[Int]).getOrElse(0) > 10000)
    //    assert(outputs._2.left.get.getMessage.contains("user does not have access to this view"))
  }
  }

  test("safeUri") { apiv3 => {
    assert(apiv3.safeUri("fffff" + apiv3.apiToken + "gggggg") === "fffffAAAXXXXXXXXXXXXgggggg")
    assert(apiv3.safeUri("fffffgggggg") === "fffffgggggg")
  }
  }

  test("constructor with config") { apiv3 => {
    val config: Config = ConfigFactory.load()
    val token = config.getString("datafinity.apiKey")
    val apiv3_2 = DatafinitiAPIv3(token)

    assert(apiv3_2.apiToken === apiv3.apiToken)


  }
  }

}
