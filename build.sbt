name := """reactive-aerospike"""

organization := "io.tabmo"

scalaVersion := "2.11.8"

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
  "-language:higherKinds",
  "-language:experimental.macros"
)

libraryDependencies ++= Seq(
  "com.aerospike" % "aerospike-client" % "3.3.2",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
  "io.github.jto" %% "validation-core" % "2.0"
)

parallelExecution in Test := false
fork in Test := false

/*
 * Publish to tabmo organization on bintray
 */
licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))

bintrayOrganization := Some("tabmo")

// Exclude logback file
mappings in (Compile, packageBin) ~= { _.filter(!_._1.getName.endsWith(".xml")) }
