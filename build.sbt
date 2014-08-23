name := "cadence"

version := "1.0"

scalaVersion := "2.10.3"

resolvers += "spray" at "http://repo.spray.io"

resolvers += "spray nightly" at "http://nightlies.spray.io/"

resolvers += "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/"

val sprayVersion = "1.3.2-20140428"
val akkaVersion = "2.3.2"

//val sprayVersion = "1.2.0"
//val akkaVersion = "2.2.3"

libraryDependencies += "io.spray" %% "spray-json" % "1.2.5"

libraryDependencies += "io.spray" % "spray-can" % sprayVersion

libraryDependencies += "io.spray" % "spray-routing" % sprayVersion

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion

libraryDependencies += "com.typesafe.slick" %% "slick" % "2.0.0"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.22"

libraryDependencies += "com.jolbox" % "bonecp" % "0.7.1.RELEASE"

libraryDependencies += "com.github.tototoshi" %% "slick-joda-mapper" % "1.1.0"

libraryDependencies += "joda-time" % "joda-time" % "2.3"

libraryDependencies += "org.joda" % "joda-convert" % "1.6"

libraryDependencies += "io.spray" % "spray-testkit" % sprayVersion % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"

libraryDependencies += "org.specs2" %% "specs2" % "2.3.13" % "test"

libraryDependencies += "com.wandoulabs.akka" %% "spray-websocket" % "0.1.2-RC1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13"

libraryDependencies += "ch.qos.logback" % "logback-core" % "1.0.13"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.5"

scalacOptions in Test ++= Seq("-Yrangepos")

Revolver.settings

Revolver.enableDebugging(port = 5050, suspend = false)

