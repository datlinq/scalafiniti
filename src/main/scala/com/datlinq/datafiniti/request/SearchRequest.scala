package com.datlinq.datafiniti.request

import com.datlinq.datafiniti.config.DatafinitiAPIFormats.{APIFormat, JSON}
import com.datlinq.datafiniti.config.DatafinitiAPIViewsV4.APIViewV4
import org.json4s.JsonDSL._
import org.json4s.{CustomSerializer, _}

/**
  * Created by Tom Lous on 01/02/2018.
  * Copyright © 2018 Datlinq B.V..
  */

object SearchRequest {

  // $COVERAGE-OFF$Seems a bit unessecary to test this
  private implicit val json4sFormats = DefaultFormats

  // $COVERAGE-ON$

  /**
    * Body params expected for /search api request
    *
    * @param query
    * @param view_name
    * @param num_records
    * @param format
    * @param download
    * @param view
    */
  case class SearchRequestV4(
                              query: String,
                              view_name: APIViewV4,
                              num_records: Option[Int] = None,
                              format: APIFormat = JSON,
                              download: Boolean = false,
                              view: Option[List[String]] = None
                            )


  /**
    * Json4s serializer for case class object to turn objects into strings and make certain fields optional
    */
  class SearchRequestSerializerV4 extends CustomSerializer[SearchRequestV4](_ => ( {
    case jsonObj: JObject =>
      val query = (jsonObj \ "query").extract[String]
      val view_name = (jsonObj \ "view_name").extract[String]
      val num_records = (jsonObj \ "num_records").extractOpt[Int]
      val format = (jsonObj \ "format").extract[String]
      val download = (jsonObj \ "download").extract[Boolean]
      val view = (jsonObj \ "view").extractOpt[List[String]]

      SearchRequestV4(query, APIViewV4.fromString(view_name), num_records, APIFormat.fromString(format), download, view)
  }, {
    case searchRequest: SearchRequestV4 =>
      ("query" -> searchRequest.query) ~
        ("view_name" -> searchRequest.view_name.toOptionString) ~
        ("num_records" -> searchRequest.num_records) ~
        ("format" -> searchRequest.format.toString) ~
        ("download" -> searchRequest.download) ~
        ("view" -> searchRequest.view)
  }
  ))

}
