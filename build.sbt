name := """reactive-aerospike"""

version := "0.1.1-SNAPSHOT"

organization := "eu.unicredit"

scalaVersion := "2.11.4"

crossScalaVersions := Seq("2.9.2", "2.11.4")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:reflectiveCalls"
)

libraryDependencies ++= Seq(
	  "com.aerospike" % "aerospike-client" % "3.0.33-SNAPSHOT",
	  "com.twitter" %% "util-collection" % "6.23.0"  
)

//Local maven repo
//"Local Aerospike Build" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
resolvers ++= Seq(
	"Local Maven Repository - snapshots" at "http://nexus.rnd.unicredit.eu/content/repositories/snapshots",
  	"Local Maven Repository - releases" at "http://nexus.rnd.unicredit.eu/content/repositories/releases"
)

publishMavenStyle := true

pomIncludeRepository := { x => false }
