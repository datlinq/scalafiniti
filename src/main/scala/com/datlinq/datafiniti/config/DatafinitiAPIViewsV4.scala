package com.datlinq.datafiniti.config

import com.datlinq.datafiniti.config.DatafinitiAPITypes._

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
object DatafinitiAPIViewsV4 {


  sealed trait APIViewV4 {
    def name: String

    def apiType: APIType

    override def toString: String = name

    def toOptionString: Option[String] = Option(name)
  }

  object APIViewV4 {
    def fromString(view_name: String): APIViewV4 = {
      view_name.split("_").toList match {
        case Nil => CustomViewV4("", APIType.fromString(""))
        case typeName :: Nil => CustomViewV4(typeName, APIType.fromString(typeName))
        case typeName :: parts => CustomViewV4(parts.mkString("_"), APIType.fromString(typeName))

        // @todo probably match agains existing objects, but probably not needed
      }
    }
  }

  case class CustomViewV4(name: String, apiType: APIType) extends APIViewV4

  /**
    * Businesses
    */

  object BusinessesDefault extends APIViewV4 {
    override val name: String = null
    override val apiType: APIType = Businesses
  }

  object BusinessesAllFlatMenus extends APIViewV4 {
    override val name: String = "business_flat_menus"
    override val apiType: APIType = Businesses
  }

  object BusinessesAllFlatReviews extends APIViewV4 {
    override val name: String = "business_flat_reviews"
    override val apiType: APIType = Businesses
  }


  object BusinessesAllNested extends APIViewV4 {
    override val name: String = "business_all_nested"
    override val apiType: APIType = Businesses
  }

  object BusinessesNoReviews extends APIViewV4 {
    override val name: String = "business_no_reviews"
    override val apiType: APIType = Businesses
  }

  object BusinessesBasic extends APIViewV4 {
    override val name: String = "business_basic"
    override val apiType: APIType = Businesses
  }

  /**
    * Products
    */
  object ProductsDefault extends APIViewV4 {
    override val name: String = null
    override val apiType: APIType = Products
  }

  object ProductsAllNested extends APIViewV4 {
    override val name: String = "product_all_nested"
    override val apiType: APIType = Products
  }


  object ProductsFlatPrices extends APIViewV4 {
    override val name: String = "product_flat_prices"
    override val apiType: APIType = Products
  }

  object ProductsFlatReviews extends APIViewV4 {
    override val name: String = "product_flat_reviews"
    override val apiType: APIType = Products
  }

  /**
    * Properties
    */
  object PropertiesDefault extends APIViewV4 {
    override val name: String = null
    override val apiType: APIType = Properties
  }

  object PropertiesFlatPrices extends APIViewV4 {
    override val name: String = "property_flat_prices"
    override val apiType: APIType = Properties
  }

  object PropertiesFlatReviews extends APIViewV4 {
    override val name: String = "property_flat_reviews"
    override val apiType: APIType = Properties
  }

}
