package com.datlinq.datafiniti.config

import com.datlinq.datafiniti.config.DatafinitiAPITypes._

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
object DatafinitiAPIViews {


  sealed trait APIView {
    protected val name: String
    val apiType: APIType

    override def toString: String = name
  }

  /**
    * Businesses
    */
  object BusinessesAll extends APIView {
    override protected val name: String = "businesses_all"
    override val apiType: APIType = Businesses
  }

  object BusinessesAllMenusFlat extends APIView {
    override protected val name: String = "businesses_all_menusFlat"
    override val apiType: APIType = Businesses
  }


  object BusinessesAllNested extends APIView {
    override protected val name: String = "businesses_all_nested"
    override val apiType: APIType = Businesses
  }

  object BusinessesAllNestedNoReviews extends APIView {
    override protected val name: String = "businesses_all_nested_no_reviews"
    override val apiType: APIType = Businesses
  }

  object BusinessesAllBasic extends APIView {
    override protected val name: String = "businesses_basic"
    override val apiType: APIType = Businesses
  }

  /**
    * Products
    */
  object ProductsAll extends APIView {
    override protected val name: String = "products_all"
    override val apiType: APIType = Products
  }

  object ProductsKeysSourceURLs extends APIView {
    override protected val name: String = "products_keysSourceURLs"
    override val apiType: APIType = Products
  }

  object ProductsMultiValuedFieldsNested extends APIView {
    override protected val name: String = "products_multiValuedFieldsNested"
    override val apiType: APIType = Products
  }

  object ProductsPricesFlat extends APIView {
    override protected val name: String = "products_pricesFlat"
    override val apiType: APIType = Products
  }

  object ProductsReviewsFlat extends APIView {
    override protected val name: String = "products_reviewsFlat"
    override val apiType: APIType = Products
  }

}
