import AssemblyKeys._
import sbtassembly.Plugin.{PathList, MergeStrategy, AssemblyKeys}

assemblySettings

val prefs = Set(jarName in assembly := "cadence.jar",
  mainClass in assembly := None,
  test in assembly := {},
  mergeStrategy in assembly <<= (mergeStrategy in assembly) {(old) =>
  {
    case PathList("org", "hamcrest", xs @ _*)         => MergeStrategy.first
    case "logback.properties" =>  MergeStrategy.discard
    case "logback.xml" =>  MergeStrategy.discard
    case "application.conf" => MergeStrategy.concat
    case x => old(x)
  }})