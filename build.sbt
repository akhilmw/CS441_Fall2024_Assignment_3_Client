ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.14"

// Version definitions - matching server
val akkaVersion = "2.8.6"
val akkaHttpVersion = "10.5.3"
val circeVersion = "0.14.9"

lazy val root = (project in file("."))
  .settings(
    name := "CS441_Fall2024_Assignment_3_Client",

    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", xs @ _*) => MergeStrategy.concat
      case PathList("META-INF", xs @ _*)             => MergeStrategy.discard
      case PathList("module-info.class")             => MergeStrategy.discard
      case PathList("META-INF", "native-image", xs @ _*) => MergeStrategy.discard
      case PathList("google", "protobuf", xs @ _*)   => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    assembly / mainClass := Some("Main"),

    libraryDependencies ++= Seq(
      // Akka dependencies
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",

      // Circe dependencies
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      // Scala Java 8 compatibility
      "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.0",

      // Logging dependencies
      "org.slf4j" % "slf4j-api" % "2.0.13",
      "ch.qos.logback" % "logback-classic" % "1.4.14",

      // Configuration
      "com.typesafe" % "config" % "1.4.3",

      // Ollama
      "io.github.ollama4j" % "ollama4j" % "1.0.79",

      // Testing
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
    ),

    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding", "utf8"
    )
  )