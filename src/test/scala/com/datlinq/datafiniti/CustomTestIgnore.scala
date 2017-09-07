package com.datlinq.datafiniti

import com.datlinq.datafiniti.config.DatafinitiAPIFormats.CSV
import com.datlinq.datafiniti.config.DatafinitiAPIViews.BusinessesAllBasic
import com.datlinq.datafiniti.response.DatafinitiTypes.DatafinitiFuture
import com.typesafe.config.{Config, ConfigFactory}
import org.json4s._
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


  test("custom do") { apiv3 => {

    val et: DatafinitiFuture[List[String]] = apiv3.downloadLinks(BusinessesAllBasic, Some("""sourceURLs:*just-eat.co.uk*"""), CSV)

    val resultList = Await.result(et.value, Duration.Inf)


    println(resultList)

    assert(condition = true)

  }
  }

}
