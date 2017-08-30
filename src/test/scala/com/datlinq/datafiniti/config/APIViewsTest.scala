package com.datlinq.datafiniti.config

import com.datlinq.datafiniti.config.APIViews._
import org.scalatest.FunSuite

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
class APIViewsTest extends FunSuite {

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

}
