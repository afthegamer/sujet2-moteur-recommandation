ThisBuild / scalaVersion := "3.8.1"
ThisBuild / version      := "0.1.0"
ThisBuild / organization := "fr.techmarket"

lazy val root = (project in file("."))
  .settings(
    name := "moteur-recommandation",
    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-feature")
  )
