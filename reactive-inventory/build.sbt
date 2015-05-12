name := "reactive-inventory"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies ++= {
  val akkaVersion       = "2.3.10"
  val akkaStreamVersion = "1.0-RC2"
  val scalaTestVersion  = "2.2.4"
  val reactiveMongoVersion = "0.10.5.0.akka23"
  val playVersion = "2.4.0-M2"
  val asyncVersion = "0.9.2"
  Seq(
    "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-stream-experimental_2.11" % akkaStreamVersion,
    "com.typesafe.akka" % "akka-http-core-experimental_2.11" % akkaStreamVersion,
    "com.typesafe.akka" % "akka-http-scala-experimental_2.11" % akkaStreamVersion,
    "org.reactivemongo" % "reactivemongo_2.11" % reactiveMongoVersion,
    "com.typesafe.play" % "play-iteratees_2.11" % playVersion,
    "org.scala-lang.modules" % "scala-async_2.11" % asyncVersion
  )
}
