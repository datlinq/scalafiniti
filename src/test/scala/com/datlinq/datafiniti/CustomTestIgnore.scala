package com.datlinq.datafiniti


import java.io.{ByteArrayOutputStream, FileOutputStream}

import com.datlinq.datafiniti.config.DatafinitiAPIFormats.{CSV, JSON}
import com.datlinq.datafiniti.config.DatafinitiAPIViewsV3
import com.datlinq.datafiniti.config.DatafinitiAPIViewsV3.{BusinessesAll, BusinessesAllBasic}
import com.datlinq.datafiniti.response.DatafinitiTypes.DatafinitiFuture
import com.typesafe.config.{Config, ConfigFactory}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.scalatest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._



/**
  * Created by Tom Lous on 07/09/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class CustomTestIgnore extends fixture.FunSuite with PrivateMethodTester {


  type FixtureParam = DatafinitiAPIv3
  implicit val json4sFormats: DefaultFormats.type = DefaultFormats

  def withFixture(test: OneArgTest): Outcome = {
    implicit val config: Config = ConfigFactory.load()
    val apiv3 = DatafinitiAPIv3()
    test(apiv3)
  }


  ignore("download") { apiv3 => {

    val target = new FileOutputStream("/tmp/justeat.co.uk.json")
    val et: DatafinitiFuture[Int] = apiv3.download(DatafinitiAPIViewsV3.BusinessesAll, Some("""sourceURLs:just-eat.co.uk"""), JSON)(target)
    val resultList = Await.result(et.value, Duration.Inf)

    target.close()

    println(resultList)


  }
  }

  ignore("download 2") { apiv3 => {

    val stream = new ByteArrayOutputStream()
    val et: DatafinitiFuture[Int] = apiv3.download(BusinessesAllBasic, Some("""categories:hotels AND city:"Den Helder""""), JSON)(stream)
    val resultCount = Await.result(et.value, Duration.Inf)

    val lines = stream.toString.split("\n")
    stream.close()

    //    val numRecords = 3

    println(resultCount)
    println(lines.flatMap(json => (parse(json) \ "city").extractOpt[String]).filter(_ == "Den Helder").toList)


  }
  }

  ignore("download just-eat.co.uk (2)") { apiv3 => {

    val et: DatafinitiFuture[JValue] = apiv3.query(
      apiView = BusinessesAll,
      query = Some("""sourceURLs:*just-eat.co.uk*"""),
      numberOfRecords = Some(30399),
      format = CSV,
      download = Some(false))

    val result = Await.result(et.value, Duration.Inf)


    val response = result.right.getOrElse(JNothing)


    println("Amount: " + (response \ "estimated total").extract[Long])


    val csv = (response \ "records").extract[String]

    val p = new java.io.PrintWriter("/tmp/justeat.csv")
    p.write(csv)
    p.close()


    println(csv)


  }
  }


  ignore("get user info") { apiv3 => {

    val et: DatafinitiFuture[Option[Long]] = apiv3.userInfoField("available_downloads")

    val resultList = Await.result(et.value, Duration.Inf)

    assert(resultList.isRight)
    assert(resultList.right.getOrElse(None).getOrElse(0L) > 0)
  }
  }

}
