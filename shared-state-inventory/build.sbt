import play.PlayScala

name := """hbc-microservice-template"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers += "Saks Artifactory - Release" at "http://repo.saksdirect.com:8081/artifactory/libs-release"

libraryDependencies ++= {
  val scalaTestVersion = "2.2.5"
  Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  )
}

net.virtualvoid.sbt.graph.Plugin.graphSettings

