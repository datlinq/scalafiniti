[![Travis](https://img.shields.io/travis/datlinq/scalafiniti.svg)](https://travis-ci.org/datlinq/scalafiniti) 
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/datlinq/scalafiniti/blob/master/LICENSE) 
[![Coverage Status](https://coveralls.io/repos/github/datlinq/scalafiniti/badge.svg?branch=master)](https://coveralls.io/github/datlinq/scalafiniti?branch=master) 
[<img src="https://img.shields.io/maven-central/v/com.datlinq/scalafiniti_2.12.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cscalafiniti)

# Scalafiniti: A Scala wrapper for Datafiniti API
The Datafiniti API is owned and maintained by Datafinity
See [datafiniti-api.readme.io](https://datafiniti-api.readme.io/)

This is an open source wrapper for that API, maintained by Datlinq

## Datafiniti API endpoints supported
- Business Data
- Product Data

## Use


### Set up SBT

Add to your build.sbt

```scala
libraryDependencies += "com.datlinq" %% "scalafiniti" % "0.1"
```

Then add import statement

```scala
import com.datlinq.datafiniti._
import com.datlinq.datafiniti.config.DatafinitiAPIFormats._
import com.datlinq.datafiniti.config.DatafinitiAPIViews._
```

Create an  APIv3 object

```scala
val apiKey = "..."
val apiv3 = DatafinitiAPIv3(apiKey)
```

Now query the API

Errors in the future or non-200 results are captured in the left part of the Either (Throwable)

Otherwise the result is parsed with json4s (even CSV requests, that return json with as CSV field) 

```scala
val response[Future[Either[Throwable,JValue]]] = apiv3.query(
  apiView = BusinessesAllBasic, 
  query = Some("categories:hotels"), 
  numberOfRecords = Some(1), 
  download = Some(false), 
  format = JSON)

```

Download flow

@todo build this

### possible Formats

* `JSON`
* `CSV` (still json result, but with one CSV field)

### possible API Views in v3

* `BusinessesAll` - businesses_all
* `BusinessesAllMenusFlat` - businesses_all_menusFlat
* `BusinessesAllNested` - businesses_all_nested
* `BusinessesAllNestedNoReviews` - businesses_all_nested_no_reviews
* `BusinessesAllBasic` - businesses_basic
* `ProductsAll` - products_all
* `ProductsKeysSourceURLs` - products_keysSourceURLs
* `ProductsMultiValuedFieldsNested` - products_multiValuedFieldsNested
* `ProductsPricesFlat` - products_pricesFlat
* `ProductsReviewsFlat` - products_reviewsFlat

## History

### Contributions
- Tom Lous (@TomLous) 

## Sample

```scala
import com.datlinq.datafiniti._
import com.datlinq.datafiniti.config.DatafinitiAPIFormats._
import com.datlinq.datafiniti.config.DatafinitiAPIViews._

import org.json4s.JsonAST.JNothing
import scala.concurrent.Await
import scala.concurrent.duration.Duration

val apiKey = "..."
val apiv3 = DatafinitiAPIv3(apiKey)

val response = apiv3.query(
  apiView = BusinessesAllBasic,
  query = Some("categories:hotels"),
  numberOfRecords = Some(1),
  download = Some(false),
  format = JSON)

val output = Await.result(response, Duration.Inf)

val json = output.getOrElse(JNothing)
```

## Compatibility

The code was build for scala 2.12 and for version v3 of the Dafaniti API