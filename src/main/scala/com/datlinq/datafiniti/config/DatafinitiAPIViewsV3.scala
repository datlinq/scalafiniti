package com.datlinq.datafiniti.config

import com.datlinq.datafiniti.config.DatafinitiAPITypes._

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
@deprecated("New v4 version now available: https://datafiniti-api.readme.io/v3/docs/migrating-from-v3-to-v4", "2017-12-15")
object DatafinitiAPIViewsV3 {


  sealed trait APIViewV3 {
    def name: String

    def apiType: APIType

    override def toString: String = name
  }

  object APIViewV3 {
    def fromString(view_name: String): APIViewV3 = {
      view_name.split("_").toList match {
        case Nil => CustomViewV3("", APIType.fromString(""))
        case typeName :: Nil => CustomViewV3(typeName, APIType.fromString(typeName))
        case typeName :: parts => CustomViewV3(parts.mkString("_"), APIType.fromString(typeName))
      }
    }
  }

  case class CustomViewV3(name: String, apiType: APIType) extends APIViewV3

  /**
    * Businesses
    */
  object BusinessesAll extends APIViewV3 {
    override val name: String = "businesses_all"
    override val apiType: APIType = Businesses
  }

  object BusinessesAllMenusFlat extends APIViewV3 {
    override val name: String = "businesses_all_menusFlat"
    override val apiType: APIType = Businesses
  }


  object BusinessesAllNested extends APIViewV3 {
    override val name: String = "businesses_all_nested"
    override val apiType: APIType = Businesses
  }

  object BusinessesAllNestedNoReviews extends APIViewV3 {
    override val name: String = "businesses_all_nested_no_reviews"
    override val apiType: APIType = Businesses
  }

  object BusinessesAllBasic extends APIViewV3 {
    override val name: String = "businesses_basic"
    override val apiType: APIType = Businesses
  }

  /**
    * Products
    */
  object ProductsAll extends APIViewV3 {
    override val name: String = "products_all"
    override val apiType: APIType = Products
  }

  object ProductsKeysSourceURLs extends APIViewV3 {
    override val name: String = "products_keysSourceURLs"
    override val apiType: APIType = Products
  }

  object ProductsMultiValuedFieldsNested extends APIViewV3 {
    override val name: String = "products_multiValuedFieldsNested"
    override val apiType: APIType = Products
  }

  object ProductsPricesFlat extends APIViewV3 {
    override val name: String = "products_pricesFlat"
    override val apiType: APIType = Products
  }

  object ProductsReviewsFlat extends APIViewV3 {
    override val name: String = "products_reviewsFlat"
    override val apiType: APIType = Products
  }

}
