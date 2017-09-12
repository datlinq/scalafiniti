package com.datlinq.datafiniti

import java.io.ByteArrayOutputStream

import com.datlinq.datafiniti.config.DatafinitiAPIFormats.{CSV, JSON}
import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import com.datlinq.datafiniti.config.DatafinitiAPIViews.{BusinessesAllBasic, ProductsAll}
import com.datlinq.datafiniti.response.DatafinitiTypes.DatafinitiFuture
import com.typesafe.config.{Config, ConfigFactory}
import org.json4s._
import org.json4s.native.JsonMethods.parse
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
  implicit val json4sFormats: DefaultFormats.type = DefaultFormats
  implicit val config: Config = ConfigFactory.load()

  def withFixture(test: OneArgTest): Outcome = {
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


  test("query") { apiv3 => {
    val compositeFuture = for {
      f1 <- apiv3.query(BusinessesAllBasic, Some("categories:hotels"), Some(1), Some(false), JSON).value
      f2 <- apiv3.query(ProductsAll, Some("categories:hotels"), Some(1), Some(false), JSON).value
      f3 <- apiv3.query(BusinessesAllBasic, Some("categories:hotels"), Some(1), Some(false), CSV).value
    } yield List(f1, f2, f3)


    val resultList = Await.result(compositeFuture, Duration.Inf)

    assert(resultList.length == 3)
    assert(resultList.head.isRight)
    //    assert(resultList(1).isLeft)
    assert(resultList(2).isRight)
    assert(resultList.head.right.map(json => (json \ "estimated total").extract[Int]).right.getOrElse(0) > 10000)
    //    assert(resultList(1).left.get.message.contains("user does not have access to this view"))
  }
  }


  test("downloadLinks") { apiv3 => {

    val et: DatafinitiFuture[List[String]] = apiv3.downloadLinks(BusinessesAllBasic, Some("""categories:hotels AND city:"Den Helder""""), JSON)

    val resultList = Await.result(et.value, Duration.Inf)


    assert(resultList.isRight)
    assert(resultList.right.map(_.length).right.getOrElse(0) > 0)
    assert(resultList.right.map(_.count(_.contains("amazonaws"))).right.getOrElse(0) > 0)

  }
  }

  test("download") { apiv3 => {

    val stream = new ByteArrayOutputStream()
    val et: DatafinitiFuture[Int] = apiv3.download(BusinessesAllBasic, Some("""categories:hotels AND city:"Den Helder""""), JSON)(stream)
    val resultCount = Await.result(et.value, Duration.Inf)

    val lines = stream.toString.split("\n")
    stream.close()

    val numRecords = 3

    assert(resultCount.right.getOrElse(-1) === numRecords)
    assert(lines.length === numRecords)
    assert(lines.flatMap(json => (parse(json) \ "city").extractOpt[String]).count(_ == "Den Helder") === numRecords)

  }
  }


  test("userInfo") { apiv3 => {

    val et: DatafinitiFuture[JValue] = apiv3.userInfo()

    val resultList = Await.result(et.value, Duration.Inf)

    assert(resultList.isRight)
    assert(resultList.right.map(json => (json \ "active").extract[Int]).right.getOrElse(0) === 1)
  }
  }


  test("userInfoField") { apiv3 => {

    val et: DatafinitiFuture[Option[Int]] = apiv3.userInfoField("active")

    val resultList = Await.result(et.value, Duration.Inf)

    assert(resultList.isRight)
    assert(resultList.right.getOrElse(None) === Some(1))
  }
  }


  test("safeUrl") { apiv3 => {
    assert(apiv3.safeUrl("fffff" + apiv3.apiToken + "gggggg") === "fffffAAAXXXXXXXXXXXXgggggg")
    assert(apiv3.safeUrl("fffffgggggg") === "fffffgggggg")
  }
  }

  test("constructor with config") { apiv3 => {
    val token = config.getString("datafinity.apiKey")
    val apiv3_2 = DatafinitiAPIv3(token)

    assert(apiv3_2.apiToken === apiv3.apiToken)
  }
  }

  test("constructor with timeout") { apiv3 => {
    implicit val config: Config = ConfigFactory.load()
    val apiv3_2 = DatafinitiAPIv3(3600)

    assert(apiv3_2.apiToken === apiv3.apiToken)
    assert(apiv3_2.httpTimeoutSeconds === apiv3.httpTimeoutSeconds)


  }
  }

}
