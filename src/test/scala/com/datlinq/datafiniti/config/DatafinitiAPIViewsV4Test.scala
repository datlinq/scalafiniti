package com.datlinq.datafiniti.config

import com.datlinq.datafiniti.config.DatafinitiAPITypes._
import com.datlinq.datafiniti.config.DatafinitiAPIViewsV4._
import org.scalatest.FunSuite

/**
  * Created by Tom Lous on 26/03/2018.
  * Copyright © 2018 Datlinq B.V..
  */
class DatafinitiAPIViewsV4Test extends FunSuite {

  test("toSting") {
    assert(BusinessesDefault.toString() === null)
    assert(BusinessesAllFlatMenus.toString() === "business_flat_menus")
    assert(BusinessesAllFlatReviews.toString() === "business_flat_reviews")
    assert(BusinessesAllNested.toString() === "business_all_nested")
    assert(BusinessesNoReviews.toString() === "business_no_reviews")
    assert(BusinessesBasic.toString() === "business_basic")

    assert(ProductsDefault.toString() === null)
    assert(ProductsAllNested.toString() === "product_all_nested")
    assert(ProductsFlatPrices.toString() === "product_flat_prices")
    assert(ProductsFlatReviews.toString() === "product_flat_reviews")

    assert(PropertiesDefault.toString() === null)
    assert(PropertiesFlatPrices.toString() === "property_flat_prices")
    assert(PropertiesFlatReviews.toString() === "property_flat_reviews")

    assert(CustomViewV4("custom_view", Businesses).toString() === "custom_view")
  }

  test("fromString") {
    assert(APIViewV4.fromString("custom_view") == CustomViewV4("view", CustomType("custom")))
    assert(APIViewV4.fromString("businesses_view") == CustomViewV4("view", Businesses))
    assert(APIViewV4.fromString("products_view_long_name") == CustomViewV4("view_long_name", Products)) // a bit hacky due to singular / plural, but low prio to fi this
  }


}
