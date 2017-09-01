name := "scalafiniti"
organization := "com.datlinq"
version := "0.1-SNAPSHOT"
isSnapshot := true

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "io.lemonlabs" %% "scala-uri" % "0.5.0",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.json4s" %% "json4s-native" % "3.5.3",

  "com.typesafe" % "config" % "1.3.1",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

// @todo move to Sonatype/MavenCentral
credentials += Credentials(
  "Artifactory Realm",
  "jfrog.datlinq.info",
  sys.env.getOrElse("JFROG_USER", ""),
  sys.env.getOrElse("JFROG_PASS", "")
)

publishTo := {
  val jfrog = "http://jfrog.datlinq.info:8081/artifactory/"
  if (isSnapshot.value)
    Some("Libs Snapshots" at jfrog + "libs-snapshot-local")
  else
    Some("Libs Releases" at jfrog + "libs-release-local")
}

resolvers +=
  "Datalabs Artifactory" at "http://jfrog.datlinq.info:8081/artifactory/libs-release-local/"

//testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")

coverageEnabled in Test := true


scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Xlint:missing-interpolator",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-language:_",
  "-encoding", "UTF-8"
)