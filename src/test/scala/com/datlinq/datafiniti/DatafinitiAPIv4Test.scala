package com.datlinq.datafiniti

import java.io.ByteArrayOutputStream

import com.datlinq.datafiniti.config.DatafinitiAPIFormats.{CSV, JSON}
import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import com.datlinq.datafiniti.config.DatafinitiAPIViewsV4._
import com.datlinq.datafiniti.request.SearchRequest._
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
class DatafinitiAPIv4Test extends fixture.FunSuite with PrivateMethodTester {

  type FixtureParam = DatafinitiAPIv4
  implicit val json4sFormats: DefaultFormats.type = DefaultFormats
  implicit val config: Config = ConfigFactory.load()

  def withFixture(test: OneArgTest): Outcome = {
    val apiv4 = DatafinitiAPIv4()
    test(apiv4)
  }



  test("private buildUrl") { apiv4 => {
    val buildUrl = PrivateMethod[String]('buildUrl)
    val token = ""

    def invoke(apiType: APIType, queryParts: Map[String, Any]): String = {
      apiv4.invokePrivate(buildUrl(apiType, queryParts)).replace(apiv4 + ":@", token)
    }

    assert(invoke(Businesses, Map.empty[String, Any]) === s"https://api.datafiniti.co/v4/businesses/search")
    assert(invoke(Products, List("a" -> 1).toMap) === s"https://api.datafiniti.co/v4/products/search?a=1")
    assert(invoke(Products, List("a" -> 1, "b" -> None, "c" -> Some(true)).toMap) === s"https://api.datafiniti.co/v4/products/search?a=1&c=true")
    assert(invoke(Businesses, List("view" -> "businesses_all", "format" -> "JSON", "q" -> Some("categories:hotels"), "records" -> 1, "download" -> false).toMap) === s"https://api.datafiniti.co/v4/businesses/search?format=JSON&q=categories:hotels&records=1&download=false&view=businesses_all")
  }
  }


  test("query") { apiv4 => {
    val compositeFuture = for {
      f1 <- apiv4.search(SearchRequestV4("categories:hotels", BusinessesBasic, Some(1), JSON)).value
      f2 <- apiv4.search(SearchRequestV4("categories:hotels", ProductsDefault, Some(1), JSON)).value
      f3 <- apiv4.search(SearchRequestV4("categories:hotels", BusinessesBasic, Some(1), CSV)).value
    } yield List(f1, f2, f3)


    val resultList = Await.result(compositeFuture, Duration.Inf)

    assert(resultList.lengthCompare(3) == 0)
    assert(resultList.head.isRight)
    //    assert(resultList(1).isLeft)
    assert(resultList(2).isRight)
    assert(resultList.head.right.map(json => (json \ "num_found").extract[Int]).right.getOrElse(0) > 10000)
    //    assert(resultList(1).left.get.message.contains("user does not have access to this view"))
  }
  }


  test("recordById") { apiv4 => {
    val ids = List("AVwdZsarkufWRAb55hYL", "AVwdE4vlIN2L1WUfr-UC", "AVwdCzdk_7pvs4fz1qog")
    //    val resultList = Await.result(apiv4.search(SearchRequestV4("categories:hotels", BusinessesBasic, Some(10), JSON)).value, Duration.Inf).map( _ \\ "id")

    val dfs: List[DatafinitiFuture[JValue]] = ids.map(id => apiv4.recordById(id, Businesses))
    val res = Await.result(Future.sequence(dfs.map(_.value)), Duration.Inf)


    val names = res.flatMap {
      case Right(json) => (json \\ "name").extractOpt[String]
      case _ => None
    }

    assert(names.sorted === List("Super 8 Las Cruces/La Posada Lane", "Motel 6", "Knights Inn").sorted)

  }
  }

  test("downloadLinks") { apiv4 => {

    val et: DatafinitiFuture[List[String]] = apiv4.downloadLinks(SearchRequestV4("""categories:hotels AND city:"Den Helder"""", BusinessesBasic, Some(1), JSON))

    val resultList = Await.result(et.value, Duration.Inf)


    assert(resultList.isRight)
    assert(resultList.right.map(_.length).right.getOrElse(0) > 0)
    assert(resultList.right.map(_.count(_.contains("amazonaws"))).right.getOrElse(0) > 0)

  }
  }

  test("download") { apiv4 => {

    val numRecords = 2
    val stream = new ByteArrayOutputStream()
    val et: DatafinitiFuture[Int] = apiv4.download(SearchRequestV4("""categories:hotels AND city:Alkmaar""", BusinessesBasic, Some(numRecords), JSON))(stream)
    val resultCount = Await.result(et.value, Duration.Inf)

    val lines = stream.toString.split("\n")
    stream.close()


    assert(resultCount.right.getOrElse(-1) === numRecords)
    assert(lines.length === numRecords)
    assert(lines.flatMap(json => (parse(json) \ "city").extractOpt[String]).count(_ == "Alkmaar") === numRecords)

  }
  }

  test("download sequential") { apiv4 => {

    val numRecords = 2
    val stream = new ByteArrayOutputStream()
    val et: DatafinitiFuture[Int] = apiv4.download(SearchRequestV4("""categories:hotels AND city:Alkmaar""", BusinessesBasic, Some(numRecords), JSON), sequential = true)(stream)
    val resultCount = Await.result(et.value, Duration.Inf)

    val lines = stream.toString.split("\n")
    stream.close()


    assert(resultCount.right.getOrElse(-1) === numRecords)
    assert(lines.length === numRecords)
    assert(lines.flatMap(json => (parse(json) \ "city").extractOpt[String]).count(_ == "Alkmaar") === numRecords)

  }
  }


  test("userInfo") { apiv4 => {

    val et: DatafinitiFuture[JValue] = apiv4.userInfo()

    val resultList = Await.result(et.value, Duration.Inf)

    assert(resultList.isRight)
    assert(resultList.right.map(json => (json \ "active").extract[Boolean]).right.getOrElse(false) === true)
  }
  }


  test("userInfoField") { apiv4 => {

    val et: DatafinitiFuture[Option[Boolean]] = apiv4.userInfoField("active")

    val resultList = Await.result(et.value, Duration.Inf)

    assert(resultList.isRight)
    assert(resultList.right.getOrElse(None) === Some(true))
  }
  }


  test("constructor with config") { apiv4 => {
    val email = config.getString("datafinity.email")
    val password = config.getString("datafinity.password")
    val apiv4_2 = DatafinitiAPIv4(email, password)

    assert(apiv4_2.email === apiv4.email)
    assert(apiv4_2.password === apiv4.password)
  }
  }

  test("constructor with timeout") { apiv4 => {
    implicit val config: Config = ConfigFactory.load()
    val apiv4_2 = DatafinitiAPIv4(3600)

    assert(apiv4_2.email === apiv4.email)
    assert(apiv4_2.password === apiv4.password)
    assert(apiv4_2.httpTimeoutSeconds === apiv4.httpTimeoutSeconds)


  }
  }

}
