name := "scalafiniti"
organization := "com.datlinq"


//enablePlugins(GitVersioning)
///* The BaseVersion setting represents the in-development (upcoming) version,
// * as an alternative to SNAPSHOTS.
// */
//git.baseVersion := "0.2.4"
//git.useGitDescribe := true
//val ReleaseTag = """^v([\d\.]+)$""".r
//git.gitTagToVersionNumber := {
//  case ReleaseTag(v) => Some(v)
//  case _ => None
//}
//
//git.formattedShaVersion := {
//  val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)
//
//  git.gitHeadCommit.value map { _.substring(0, 7) } map { sha =>
//    git.baseVersion.value + "-" + sha + suffix
//  }
//}
isSnapshot := version.value endsWith "SNAPSHOT"


scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.12.4", "2.11.12")

/* Build */
libraryDependencies ++= Seq(
  "io.lemonlabs" %% "scala-uri" % "0.5.3",
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "org.json4s" %% "json4s-native" % "3.5.3",

  "com.typesafe" % "config" % "1.3.2",
  "org.typelevel" %% "cats-core" % "1.0.1",


  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "org.scalatest" %% "scalatest" % "3.0.4" % "test"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Xlint:missing-interpolator",
  "-Ywarn-unused",
  "-Ywarn-dead-code",
  "-Ypartial-unification",
  "-language:_",
  "-encoding", "UTF-8"
)

/* Test */

coverageEnabled in Test := true

testOptions in Test := Seq(Tests.Filter(!_.endsWith("Ignore")))


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
    Opts.resolver.sonatypeStaging
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




