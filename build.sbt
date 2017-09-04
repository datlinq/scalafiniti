name := "scalafiniti"
organization := "com.datlinq"


//enablePlugins(GitVersioning)
//git.baseVersion := "0.1"
//version := "0.1.1"
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

coverageEnabled in Test := true


/* Publish */

// Sonatype
credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  sys.env.getOrElse("SONATYPE_USER", ""),
  sys.env.getOrElse("SONATYPE_PASS", "")
)

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeReleases
)


publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

useGpg := false
usePgpKeyHex("614567F6305DA15D")
pgpPublicRing := baseDirectory.value / "project" / "gnupg" / "pubring.gpg"
pgpSecretRing := baseDirectory.value / "project" / "gnupg" / "secring.gpg"
pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray)


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




