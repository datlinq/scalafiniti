package com.datlinq.datafiniti.request

import com.datlinq.datafiniti.config.DatafinitiAPIFormats.JSON
import com.datlinq.datafiniti.config.DatafinitiAPIViewsV4.BusinessesAllNested
import com.datlinq.datafiniti.request.SearchRequest.{SearchRequestSerializerV4, SearchRequestV4}
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, Formats, _}
import org.scalatest.FunSuite


/**
  * Created by Tom Lous on 26/03/2018.
  * Copyright © 2018 Datlinq B.V..
  */
class SearchRequestTest extends FunSuite {


  implicit val json4sFormats: Formats = DefaultFormats + new SearchRequestSerializerV4()

  val s = SearchRequestV4(
    query = "queryData",
    view_name = BusinessesAllNested,
    num_records = Some(100),
    format = JSON,
    download = true,
    view = Some(List("view1", "view2"))
  )


  test("SearchRequestV4") {
    assert(s.query === "queryData")
    assert(s.view_name === BusinessesAllNested)
    assert(s.num_records.get === 100)
    assert(s.format === JSON)
    assert(s.download === true)
    assert(s.view === Some(List("view1", "view2")))
  }


  test("json4sFormats") {
    assert(SearchRequest.json4sFormats === DefaultFormats)
  }


  test("SearchRequestSerializerV4") {
    val json = parse(write(s))

    assert((json \ "query").extract[String] === s.query)
    assert((json \ "view_name").extract[String] === s.view_name.name)
    assert((json \ "num_records").extractOpt[Int] === s.num_records)
    assert((json \ "format").extract[String] === s.format.name)
    assert((json \ "download").extract[Boolean] === s.download)
    assert((json \ "view").extract[List[String]] === s.view.get)
  }


}
