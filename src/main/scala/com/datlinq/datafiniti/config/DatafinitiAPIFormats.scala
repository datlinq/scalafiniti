package com.datlinq.datafiniti.config


/**
  * Created by Tom Lous on 30/08/2017.
  * Copyright © 2017 Datlinq B.V..
  */
object DatafinitiAPIFormats {

  sealed trait APIFormat {
    def name: String

    override def toString: String = name
  }

  object APIFormat {
    def fromString(format: String): APIFormat = {
      if (format.toLowerCase == JSON.name.toLowerCase) JSON
      else if (format.toLowerCase == CSV.name.toLowerCase) CSV
      else CustomFormat(format)
    }
  }

  case class CustomFormat(name: String) extends APIFormat

  object JSON extends APIFormat {
    override val name: String = "JSON"
  }

  object CSV extends APIFormat {
    override val name: String = "CSV"

  }

}
