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
- Property Data

## Use


### Set up SBT

Add to your build.sbt

```scala
libraryDependencies += "com.datlinq" %% "scalafiniti" % "0.3.4"
```

Then add import statement

```scala
import com.datlinq.datafiniti._
import com.datlinq.datafiniti.config.DatafinitiAPIFormats._
import com.datlinq.datafiniti.config.DatafinitiAPIViewsV4._
import com.datlinq.datafiniti.config.DatafinitiAPITypes._
```

Create an  APIv4 object

```scala
val email = "..."
val password = "..."
val apiv4 = DatafinitiAPIv4(email, password)
```


#### Query

Now query the API

Errors in the future or non-200 results are captured in DatafinitiErrors

Otherwise the result is parsed with json4s (even CSV requests, that return json with as CSV field)

The async calls return an DatafinitiFuture[T] which are [EitherT](https://typelevel.org/cats/api/cats/data/EitherT.html)\[Future,DatafinitiError,JValue]\] (see cats library)

This makes the Eithers in Futures composable in for comprehensions and such

```scala
val response:DatafinitiFuture[JValue] = apiv4.search(
  SearchRequestV4(
    query = "categories:hotels",
    view_name = BusinessesBasic,
    records = Some(1),
    format = JSON,
    download = false,
    view = None
    ))    
```


#### Record by Id

To get an individual record from Datafiniti there is a specific call `recordById`
The main difference is that it's a Restful GET request with id in url.
The SDK syntax is similar to the query, but simpler.

Only a id as `String` and a specific `APIType` are required

```scala
val response:DatafinitiFuture[JValue] = apiv4.recordById("xxxxxxxxx", Businesses)
```
 

#### Download

The Download flow contains of multiple API calls first triggering the download, then redirect to polling API call untill the download is marked as COMPLETED afterwards redirecting to a similar API call to fetch the download URL's
The method `downloadLinks` returns a List of Strings wrapped in a DatafinitiFuture.

```scala
val response:DatafinitiFuture[List[String]] = apiv4.downloadLinks(
  SearchRequestV4(
      query = """categories:hotels AND city:"Capelle aan den IJssel"""",
      view_name = BusinessesBasic,
      records = Some(1),
      format = JSON,
      download = true,
      view = None
  ))        
```

or download all files directly to a stream. Pass an outputstream to append lines, beware that resulting file may have records be out of order if there are multiple download files in the response, to prevent this set sequential to true (will be slower).
The returned integer contains the total count of all (or limited by numberOfRecords) imported records

```scala
val response:DatafinitiFuture[Int] = apiv4.download(
    SearchRequestV4("""categories:hotels AND city:Rotterdam""", BusinessesBasic, numRecords, JSON), sequential = true)(stream)
```


#### User info
User information, including access rights and remaining requests are accessed by the

userInfo, returning the entire Json (or sub json if string was passed)

```scala
val et: DatafinitiFuture[JValue] = apiv4.userInfo()
```

and userInfoField, returning a specificly extracted value from user info (like  "available_downloads")


```scala
val et: DatafinitiFuture[Option[Long]] = apiv4.userInfoField("available_downloads")
```



### possible Formats

* `JSON`
* `CSV` (basic query still returns json result, but with one field filled with CSV data)

### possible API Views in v4

* `BusinessesDefault` - null
* `BusinessesAllFlatMenus` - business_flat_menus"
* `BusinessesAllFlatReviews` - business_flat_reviews"
* `BusinessesAllNested` - business_all_nested"
* `BusinessesNoReviews` - business_no_reviews"
* `BusinessesBasic` - business_basic"
* `ProductsDefault` - null
* `ProductsAllNested` - product_all_nested"
* `ProductsFlatPrices` - product_flat_prices"
* `ProductsFlatReviews` - product_flat_reviews"
* `PropertiesDefault` - null
* `PropertiesFlatPrices` - property_flat_prices"
* `PropertiesFlatReviews` - property_flat_reviews"

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
    import com.datlinq.datafiniti.config.DatafinitiAPIFormats._
    import com.datlinq.datafiniti.config.DatafinitiAPITypes._
    import com.datlinq.datafiniti.config.DatafinitiAPIViewsV4._
    import com.datlinq.datafiniti.request.SearchRequest.SearchRequestV4
    import org.json4s.JsonAST.JNothing

    import scala.concurrent.Await
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration.Duration


    val email = "...."
    val password = "...."
    val apiv4 = DatafinitiAPIv4()

    // query
    val futureEither = apiv4.search(
      SearchRequestV4("""categories:restaurant AND city:Bergschenhoek""", BusinessesAllNested, Some(10), JSON))

    val result = Await.result(futureEither.value, Duration.Inf)

    val json = result.right.getOrElse(JNothing)

    // recordById
    val futureEither2 = apiv4.recordById("AWEF3R5B3-Khe5l_drZz", Businesses)

    val result2 = Await.result(futureEither2.value, Duration.Inf)

    val json2 = result2.right.getOrElse(JNothing)


    // download links
    val futureEither3 = apiv4.downloadLinks(
      SearchRequestV4("""categories:restaurant AND city:Lansingerland""", BusinessesAllNested)
    )

    val result3 = Await.result(futureEither3.value, Duration.Inf)

    val links = result3.right.getOrElse(Nil)


    // download
    val stream = new FileOutputStream("/tmp/output.json")

    val futureEither4 = apiv4.download(
      SearchRequestV4(
        view_name = BusinessesAllNested,
        query = """categories:restaurant AND city:"Berkel en Rodenrijs"""",
        format = JSON,
        num_records = None
      ),
      sequential = false
    )(stream)

    val result4 = Await.result(futureEither4.value, Duration.Inf)

    stream.close()
```

## Compatibility

The code was build for scala 2.11, 2.12 and for version v3 & v4 of the Dafaniti API