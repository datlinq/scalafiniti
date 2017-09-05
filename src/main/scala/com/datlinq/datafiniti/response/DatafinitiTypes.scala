package com.datlinq.datafiniti.response

import cats.data.EitherT

import scala.concurrent.Future

/**
  * Created by Tom Lous on 05/09/2017.
  * Copyright © 2017 Datlinq B.V..
  */
object DatafinitiTypes {

  type DatafinitiFuture[T] = EitherT[Future, DatafinitiError, T]
  type DatafinitiResponse[T] = Either[DatafinitiError, T]

}
