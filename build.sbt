ThisBuild / version := "0.1.1"

ThisBuild / scalaVersion := "2.11.12"

libraryDependencies += "com.lihaoyi" %% "ujson" % "0.7.1"
libraryDependencies += "com.lihaoyi" %% "requests" % "0.7.0"
libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.8.0"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.3.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.3.0"
libraryDependencies += "org.apache.spark" %% "spark-hive" % "2.3.0"


lazy val root = (project in file("."))
  .settings(
    name := "gamesdb"
  )
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
