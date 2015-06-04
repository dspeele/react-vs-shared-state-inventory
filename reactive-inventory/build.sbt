import play.PlayScala

name := "reactive-inventory"

version := "1.0-SNAPSHOT"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

libraryDependencies ++= {
  val scalaTestVersion     = "2.2.5"
  val reactiveMongoVersion = "0.10.5.0.akka23"
  val akkaVersion          = "2.3.10"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "org.reactivemongo" %% "reactivemongo" % reactiveMongoVersion
  )
}
