
lazy val log4j2appenders = (project in file(".")).settings(
  name:= "log4j2appenders",
  scalaVersion := "2.12.6",
  crossScalaVersions := Seq("2.11.12"),
  version := "0.0.1",
  libraryDependencies ++= Seq(
    // Logging
    log4j2_api, log4j2_core, slf4j, log4j2_slf4j_impl,
    // AWS SQS
    aws_sqs),
  excludeDependencies ++= Seq(
    ExclusionRule("org.slf4j", "slf4j-log4j12"),
    ExclusionRule("ch.qos.logback", "logback-core"),
    ExclusionRule("ch.qos.logback", "logback-classic")
  )
)

// log4j2
val log4j2_api = "org.apache.logging.log4j" % "log4j-api" % "2.16.0"
val log4j2_core = "org.apache.logging.log4j" % "log4j-core" % "2.16.0"
val log4j2_slf4j_impl = "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.16.0"
// slf4j
val slf4j = "org.slf4j" % "slf4j-api" % "1.7.25"
// aws
//val awscala = "com.github.seratch" %% "awscala" % "0.8.1"
val aws_sqs = "com.amazonaws" % "aws-java-sdk-sqs" % "1.11.482"