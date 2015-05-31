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
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-scala-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-testkit-scala-experimental" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamVersion,
    "org.reactivemongo" %% "reactivemongo" % reactiveMongoVersion,
    "com.typesafe.play" %% "play-iteratees" % playVersion,
    "org.scala-lang.modules" %% "scala-async" % asyncVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion,
    "com.typesafe.play" %% "play-test" % playVersion,
    "com.typesafe.play" %% "play" % playVersion,
    "com.typesafe.play" %% "play-json" % playVersion,
    "com.github.simplyscala" %% "scalatest-embedmongo" % testEmbedMongoVersion
  )
}
