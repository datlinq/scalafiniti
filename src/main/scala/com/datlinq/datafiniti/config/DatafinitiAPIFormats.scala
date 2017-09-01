package com.datlinq.datafiniti.config


/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
object DatafinitiAPIFormats {

  sealed trait APIFormat {
    protected val name: String

    override def toString: String = name
  }

  object JSON extends APIFormat {
    override protected val name: String = "JSON"
  }

  object CSV extends APIFormat {
    override protected val name: String = "CSV"

  }

}
