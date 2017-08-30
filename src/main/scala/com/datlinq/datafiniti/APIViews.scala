package com.datlinq.datafiniti

import com.datlinq.datafiniti.APITypes._

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
object APIViews {


  sealed trait APIView {
    val name: String
    val apiType: APIType

    override def toString: String = name
  }

  object BusinessesAll extends APIView {
    val name = "businesses_all"
    val apiType = Businesses
  }


  //  businesses_all
  //  businesses_all_menusFlat
  //  businesses_all_nested
  //  businesses_all_nested_no_reviews
  //  businesses_basic
  //  products_all
  //  products_keysSourceURLs
  //  products_multiValuedFieldsNested
  //  products_pricesFlat
  //  products_reviewsFlat
}
