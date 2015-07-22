import SonatypeKeys._

name := """reactive-aerospike"""

version := "1.0.0"

organization := "io.tabmo"

scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  "-deprecation",           // Warn when deprecated API are used
  "-feature",               // Warn for usages of features that should be importer explicitly
  "-unchecked",             // Warn when generated code depends on assumptions
  "-Ywarn-dead-code",       // Warn when dead code is identified
  "-Ywarn-numeric-widen",   // Warn when numeric are widened
  "-Xlint",                 // Additional warnings (see scalac -Xlint:help)
  "-Ywarn-adapted-args",    // Warn if an argument list is modified to match the receive
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:existentials",
  "-language:higherKinds"

)

libraryDependencies ++= Seq(
  "com.aerospike" % "aerospike-client" % "3.1.3",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
)

publishMavenStyle := true

parallelExecution in Test := false

pomIncludeRepository := { x => false }

sonatypeSettings

credentials += Credentials(Path.userHome / ".ivy2" / "sonatype.credentials")
