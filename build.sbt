name := """reactive-aerospike"""

version := "0.1.1-SNAPSHOT"

organization := "eu.unicredit"

crossScalaVersions := Seq("2.9.2", "2.10.4")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:reflectiveCalls"
)

libraryDependencies ++= Seq(
	  "org.luaj" % "luaj-jse" % "3.0",
	  "org.gnu" % "gnu-crypto" % "2.0.1",
	  "org.mindrot" % "jbcrypt" % "0.3m"
)

resolvers ++= Seq(
)

publishMavenStyle := true

pomIncludeRepository := { x => false }
