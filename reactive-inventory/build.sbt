name := "reactive-inventory"

version := "1.0"

scalaVersion := "2.11.5"

resolvers +="Typesafe - Release" at "http://repo.typesafe.com/typesafe/releases"

libraryDependencies ++= {
  val akkaVersion       = "2.3.10"
  val akkaStreamVersion = "1.0-RC2"
  val scalaTestVersion  = "2.3.0-SNAP2"
  val reactiveMongoVersion = "0.10.5.0.akka23"
  val playVersion = "2.3.0"
  val asyncVersion = "0.9.2"
  val testEmbedMongoVersion = "0.2.2"
  Seq(
    "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-testkit_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-stream-experimental_2.11" % akkaStreamVersion,
    "com.typesafe.akka" % "akka-http-core-experimental_2.11" % akkaStreamVersion,
    "com.typesafe.akka" % "akka-http-scala-experimental_2.11" % akkaStreamVersion,
    "com.typesafe.akka" % "akka-http-testkit-scala-experimental_2.11" % akkaStreamVersion,
    "com.typesafe.akka" % "akka-http-spray-json-experimental_2.11" % akkaStreamVersion,
    "org.reactivemongo" % "reactivemongo_2.11" % reactiveMongoVersion,
    "com.typesafe.play" % "play-iteratees_2.11" % playVersion,
    "org.scala-lang.modules" % "scala-async_2.11" % asyncVersion,
    "org.scalatest" % "scalatest_2.11" % scalaTestVersion,
    "com.typesafe.play" % "play-test_2.11" % playVersion,
    "com.typesafe.play" % "play_2.11" % playVersion,
    "com.typesafe.play" % "play-json_2.11" % playVersion,
    "com.github.simplyscala" % "scalatest-embedmongo_2.11" % testEmbedMongoVersion
  )
}
