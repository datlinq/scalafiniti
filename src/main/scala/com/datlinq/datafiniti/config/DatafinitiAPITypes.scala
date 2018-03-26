package com.datlinq.datafiniti.config

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */

object DatafinitiAPITypes {

  sealed trait APIType {
    def name: String

    override def toString: String = name
  }

  object APIType {
    def fromString(typeName: String): APIType = {
      if (typeName.toLowerCase == Businesses.name.toLowerCase) Businesses
      else if (typeName.toLowerCase == Products.name.toLowerCase) Products
      else if (typeName.toLowerCase == Properties.name.toLowerCase) Properties
      else CustomType(typeName)
    }
  }

  case class CustomType(name: String) extends APIType

  object Businesses extends APIType {
    override val name: String = "businesses"
  }

  object Products extends APIType {
    override val name: String = "products"
  }


  object Properties extends APIType {
    override val name: String = "properties"
  }


}