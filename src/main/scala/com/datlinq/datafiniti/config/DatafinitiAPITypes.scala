package com.datlinq.datafiniti.config

/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */

object DatafinitiAPITypes {

  sealed trait APIType {
    protected val name: String

    override def toString: String = name
  }

  object Businesses extends APIType {
    override protected val name: String = "businesses"
  }

  object Products extends APIType {
    override protected val name: String = "products"
  }


}