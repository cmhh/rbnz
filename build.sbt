lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.cmhh",
      scalaVersion := "2.13.8",
      version      := "0.1.0"
    )),

    name := "rbnz",
    
    libraryDependencies ++= Seq(
      "org.seleniumhq.selenium" % "selenium-java" % "3.141.59",
      "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.141.59",
      "org.apache.poi" % "poi" % "5.2.2",
      "org.apache.poi" % "poi-ooxml" % "5.2.2",
      "com.typesafe.akka" %% "akka-actor" % "2.6.19",
      "com.typesafe.akka" %% "akka-http" % "10.2.9",
      "com.typesafe.akka" %% "akka-stream" % "2.6.19",
      "org.xerial" % "sqlite-jdbc" % "3.36.0.3"
    ),

    scalacOptions += "-deprecation",
    
    assembly / assemblyJarName := "rbnz.jar",
    
    ThisBuild / assemblyMergeStrategy := {
      case PathList("reference.conf") => MergeStrategy.concat
      case "reference.conf" => MergeStrategy.concat
      case "application.conf" => MergeStrategy.concat
      case n if n.contains("services") => MergeStrategy.concat
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
  )
