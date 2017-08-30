package com.datlinq.datafiniti

import com.datlinq.datafiniti.APITypes._
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
case class APIv3(apiToken: String) extends API with LazyLogging {

  protected val VERSION = "v3"


  /**
    *
    * # Set your API parameters here.
    * APIToken = 'AAAXXXXXXXXXXXX'
    * view = 'businesses_all'
    * format = 'JSON'
    * query = urllib.parse.quote_plus('categories:hotels')
    * records = '1'
    * download = 'false'
    * *
    * # Construct the API call.
    * APICall = 'https://' + APIToken + ':@api.datafiniti.co/v3/data/businesses?' \
    * + 'view=' + view \
    * + '&format=' + format \
    * + '&q=' + query \
    * + '&records=' + records \
    * + '&download=' + download
    *
    * @return
    */


  private def buildUrl(apiType: APIType, queryParts: Map[String, Any]): String = {
    queryParts.foldLeft(s"$SCHEME://$apiToken:@$DOMAIN/$VERSION/data/$apiType")((uri, keyVal) => {
      uri & keyVal
    }).toString
  }

  def query(apiType: APIType, view: Option[String], format: Option[String] = None, query: Option[String] = None, numberOfRecord: Option[Int] = None, download: Option[Boolean] = None) = {
    val url = buildUrl(apiType, List("view" -> view, "format" -> format, "q" -> query, "numberOfRecord" -> numberOfRecord, "download" -> download).toMap)

    println(url)
  }


}
