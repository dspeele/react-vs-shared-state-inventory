import play.PlayScala

name := "shared-state-inventory"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

resolvers += "Saks Artifactory - Release" at "http://repo.saksdirect.com:8081/artifactory/libs-release"

libraryDependencies ++= {
  val scalaTestVersion = "2.2.5"
  val reactiveMongoVersion = "0.10.5.0.akka23"
  Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.reactivemongo" %% "reactivemongo" % reactiveMongoVersion
  )
}

