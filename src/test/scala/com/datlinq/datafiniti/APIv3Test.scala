package com.datlinq.datafiniti

import com.datlinq.datafiniti.APITypes._
import com.typesafe.config.ConfigFactory
import org.scalatest.{Outcome, fixture}

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class APIv3Test extends fixture.FunSuite {

  type FixtureParam = APIv3

  def withFixture(test: OneArgTest): Outcome = {
    val config = ConfigFactory.load()
    val apiKey = config.getString("apiKey")
    val apiv3 = APIv3(apiKey)
    test(apiv3)
  }


  test("query empty") { apiv3 => {

    apiv3.query(Businesses, None)

  }
  }


  // @todo shitload more tests

}
