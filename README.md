[![Travis](https://img.shields.io/travis/datlinq/scalafiniti.svg)](https://travis-ci.org/datlinq/scalafiniti)
[![license](https://img.shields.io/github/license/mashape/apistatus.svg)](https://github.com/datlinq/scalafiniti/blob/master/LICENSE)
[![Coverage Status](https://coveralls.io/repos/github/datlinq/scalafiniti/badge.svg?branch=master)](https://coveralls.io/github/datlinq/scalafiniti?branch=master) 
[<img src="https://img.shields.io/maven-central/v/com.datlinq/scalafiniti_2.12.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cscalafiniti)

# Scalafiniti: A Scala wrapper for Datafiniti API
The Datafiniti API is owned and maintained by Datafinity
See [datafiniti-api.readme.io](https://datafiniti-api.readme.io/)

This is an open source wrapper for that API, maintained by [Datlinq](http://datlinq.com)

## Datafiniti API endpoints supported
- Business Data
- Product Data

## Use


### Set up SBT

Add to your build.sbt

```scala
libraryDependencies += "com.datlinq" %% "scalafiniti" % "0.2.5"
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


#### Query

Now query the API

Errors in the future or non-200 results are captured in DatafinitiErrors

Otherwise the result is parsed with json4s (even CSV requests, that return json with as CSV field)

The async calls return an DatafinitiFuture[T] which are [EitherT](https://typelevel.org/cats/api/cats/data/EitherT.html)\[Future,DatafinitiError,JValue]\] (see cats library)

This makes the Eithers in Futures composable in for comprehensions and such

```scala
val response:DatafinitiFuture[JValue] = apiv3.query(
  apiView = BusinessesAllBasic, 
  query = Some("categories:hotels"), 
  numberOfRecords = Some(1), 
  download = Some(false), 
  format = JSON)
```

#### Download

The Download flow contains of multiple API calls first triggering the download, then redirect to polling API call untill the download is marked as COMPLETED afterwards redirecting to a similar API call to fetch the download URL's
The method `downloadLinks` returns a List of Strings wrapped in a DatafinitiFuture.

```scala
val response:DatafinitiFuture[List[String]] = apiv3.downloadLinks(
    apiView = BusinessesAllNested,
    query = Some("""categories:hotels AND city:"Rotterdam""""),
    format = JSON,
    numRecords = None)
```

or download all files directly to a stream. Pass an outputstream to append lines, beware that resulting file may have records be out of order if there are multiple download files in the response.
The returned integer contains the total count of all (or limited by numberOfRecords) imported records

```scala
val response:DatafinitiFuture[Int] = apiv3.download(
    apiView = BusinessesAllNested,
    query = Some("""categories:hotels AND city:"Rotterdam""""),
    format = JSON,
    numberOfRecords = None)(stream)
```


#### User info
User information, including access rights and remaining requests are accessed by the

userInfo, returning the entire Json (or sub json if string was passed)

```scala
val et: DatafinitiFuture[JValue] = apiv3.userInfo()
```

and userInfoField, returning a specificly extracted value from user info (like  "available_downloads")


```scala
val et: DatafinitiFuture[Option[Long]] = apiv3.userInfoField("available_downloads")
```



### possible Formats

* `JSON`
* `CSV` (basic query still returns json result, but with one field filled with CSV data)

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

### The current maintainers (people who can help you) are:

- Tom Lous ([@tomlous](https://github.com/TomLous))

## Sample

```scala
import com.datlinq.datafiniti._
import com.datlinq.datafiniti.config.DatafinitiAPIFormats._
import com.datlinq.datafiniti.config.DatafinitiAPIViews._

import org.json4s.JsonAST.JNothing
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import java.io.FileOutputStream

import scala.concurrent.ExecutionContext.Implicits.global



val apiKey = "...."
val apiv3 = DatafinitiAPIv3(apiKey)

// query
val futureEither = apiv3.query(
  apiView = BusinessesAllNested,
  query = Some("""categories:hotels AND city:"Den Helder""""),
  numberOfRecords = Some(10),
  download = Some(false),
  format = JSON)

val result = Await.result(futureEither.value, Duration.Inf)

val json = result.getOrElse(JNothing)



// download links
val futureEither2 = apiv3.downloadLinks(
  apiView = BusinessesAllNested,
  query = Some("""categories:hotels AND city:"Den Helder""""),
  format = JSON,
  numberOfRecords = None
)

val result2 = Await.result(futureEither2.value, Duration.Inf)

val links = result2.getOrElse(Nil)


// download
val stream = new FileOutputStream("/tmp/output.json")

val futureEither3 = apiv3.downloadLinks(
  apiView = BusinessesAllNested,
  query = Some("""categories:hotels AND city:"Den Helder""""),
  format = JSON,
  numberOfRecords = None
)(stream)

val result3 = Await.result(futureEither3.value, Duration.Inf)

stream.close()


```

## Compatibility

The code was build for scala 2.11, 2.12 and for version v3 of the Dafaniti API