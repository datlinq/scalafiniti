

name := "scalafiniti"
organization := "com.datlinq"


version := "0.1"
isSnapshot := true

scalaVersion := "2.12.3"


/* Build */
libraryDependencies ++= Seq(
  "io.lemonlabs" %% "scala-uri" % "0.5.0",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.json4s" %% "json4s-native" % "3.5.3",

  "com.typesafe" % "config" % "1.3.1",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

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

/* Test */

//testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")
coverageEnabled in Test := true


/* Publish */


/*
// Datlinq local
resolvers +=
  "Datalabs Artifactory" at "http://jfrog.datlinq.info:8081/artifactory/libs-release-local/"

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
*/

// Sonatype
credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  sys.env.getOrElse("SONATYPE_USER", ""),
  sys.env.getOrElse("SONATYPE_PASS", "")
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}



publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }
useGpg := true
usePgpKeyHex("614567F6305DA15D")
pgpPassphrase in ThisBuild := sys.env.get("PGP_PASS").map(_.toArray)
credentials += Credentials(file("gpg.credentials"))

//pgpSecretRing := file("./scripts/datalabs.asc")
//PgpKeys.useGpg := true
//PgpKeys.pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray)


licenses := Seq(("MIT", url("http://opensource.org/licenses/MIT")))
homepage := Some(url("https://github.com/datlinq/scalafiniti"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/datlinq/scalafiniti"),
    "scm:git@github.com:datlinq/scalafiniti.git"
  )
)

developers := List(
  Developer(
    id = "TomLous",
    name = "Tom Lous",
    email = "tomlous@gmail.com",
    url = url("https://about.me/tomlous")
  )
)




