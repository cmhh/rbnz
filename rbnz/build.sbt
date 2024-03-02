lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.cmhh",
      scalaVersion := "2.13.12",
      version      := "0.4.0"
    )),

    name := "rbnz",
    
    libraryDependencies ++= Seq(
      "org.seleniumhq.selenium" % "selenium-java" % "3.141.59", // "3.141.59" "4.18.1"
      "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.141.59", // "3.141.59" "4.18.1"
      "org.apache.poi" % "poi" % "5.2.5",
      "org.apache.poi" % "poi-ooxml" % "5.2.5",
      "com.typesafe.akka" %% "akka-actor" % "2.8.5",
      "com.typesafe.akka" %% "akka-http" % "10.5.3",
      "com.typesafe.akka" %% "akka-stream" % "2.8.5",
      "org.xerial" % "sqlite-jdbc" % "3.45.1.0",
      "org.rogach" %% "scallop" % "5.0.1"
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
