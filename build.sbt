lazy val commonSettings = Seq(
  scalaVersion := "2.11.7", // "2.12.0-M3",
  version      := "1.0",
  licenses     := Seq("New BSD" -> url("https://raw.githubusercontent.com/scaled/scaled/master/LICENSE"))
)

lazy val std = Project(id = "scaled-std", base = file("std"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "18.0",
      "net.sourceforge.findbugs" % "jsr305" % "1.3.7"
    )
  )

lazy val api = Project(id = "scaled-api", base = file("api"))
  .dependsOn(std)
  .settings(commonSettings)

lazy val pacman = ProjectRef(uri("git://github.com/Sciss/pacman.git#sbtfied"), "pacman")

lazy val editor = Project(id = "scaled-editor", base = file("editor"))
  .dependsOn(api, pacman)
  .settings(commonSettings)
  .settings(
    libraryDependencies += "org.ow2.asm" % "asm" % "5.0.1"
  )

lazy val root = Project(id = "scaled", base = file("."))
  .dependsOn(std, api, editor)
  .aggregate(std, api, editor)
  .settings(commonSettings)
  .settings(
    description := "A Scalable editor, extensible via JVM languages",
    mainClass in Compile := Some("scaled.impl.Scaled"),
    fork in run := true
  )
