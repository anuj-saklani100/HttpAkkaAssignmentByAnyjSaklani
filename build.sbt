ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.8"

lazy val root = (project in file("."))
  .settings(
    name := "newhttppractice"
  )
val akkaVersion = "2.5.20"
val akkaHttpVersion= "10.1.7"
val scalaTestVersion= "3.0.5"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.pauldijou" %% "jwt-spray-json" % "2.1.0",
  "org.scalatest" %% "scalatest" % scalaTestVersion,
   "mysql" % "mysql-connector-java" % "8.0.26"

)
