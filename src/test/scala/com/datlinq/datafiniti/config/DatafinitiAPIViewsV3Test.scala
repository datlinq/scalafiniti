package com.datlinq.datafiniti.config

import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import com.datlinq.datafiniti.config.DatafinitiAPIViewsV3._
import org.scalatest.FunSuite

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
@deprecated("New v4 version now available: https://datafiniti-api.readme.io/v3/docs/migrating-from-v3-to-v4", "2017-12-15")
class DatafinitiAPIViewsV3Test extends FunSuite {

  test("toSting") {
    assert(BusinessesAll.toString() === "businesses_all")

    assert(BusinessesAllMenusFlat.toString() === "businesses_all_menusFlat")


    assert(BusinessesAllNested.toString() === "businesses_all_nested")

    assert(BusinessesAllNestedNoReviews.toString() === "businesses_all_nested_no_reviews")

    assert(BusinessesAllBasic.toString() === "businesses_basic")


    assert(ProductsAll.toString() === "products_all")

    assert(ProductsKeysSourceURLs.toString() === "products_keysSourceURLs")

    assert(ProductsMultiValuedFieldsNested.toString() === "products_multiValuedFieldsNested")

    assert(ProductsPricesFlat.toString() === "products_pricesFlat")

    assert(ProductsReviewsFlat.toString() === "products_reviewsFlat")
  }

  test("fromString") {
    assert(APIViewV3.fromString("custom_view") == CustomViewV3("view", CustomType("custom")))
    assert(APIViewV3.fromString("businesses_view") == CustomViewV3("view", Businesses))
    assert(APIViewV3.fromString("products_view_long_name") == CustomViewV3("view_long_name", Products)) // a bit hacky due to singular / plural, but low prio to fi this
  }

}
