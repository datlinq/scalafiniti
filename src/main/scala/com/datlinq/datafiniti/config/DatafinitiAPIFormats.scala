package com.datlinq.datafiniti.config


/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
object DatafinitiAPIFormats {

  sealed trait APIFormat {
    protected val name: String
    //    val asResponse: (Response => Any)

    override def toString: String = name
  }

  object JSON extends APIFormat {
    override protected val name: String = "JSON"
    //    override val asResponse: (Response) => JValue = dispatch.as.json4s.Json
  }

  object CSV extends APIFormat {
    override protected val name: String = "CSV"
    //    override val asResponse: (Response) => String = dispatch.as.String
  }

}
