package com.datlinq.datafiniti.config

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */

object APITypes {

  sealed trait APIType {
    val name: String

    override def toString: String = name
  }

  object Businesses extends APIType {
    override val name: String = "businesses"
  }

  object Products extends APIType {
    override val name: String = "products"
  }


}