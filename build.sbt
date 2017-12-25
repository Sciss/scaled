lazy val commonSettings = Seq(
  scalaVersion := "2.12.4",
  version      := "0.1.0-SNAPSHOT",
  licenses     := Seq("New BSD" -> url("https://raw.githubusercontent.com/scaled/scaled/master/LICENSE")),
  organization := "de.sciss"
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

// lazy val pacman = ProjectRef(uri("git://github.com/Sciss/pacman.git#sbtfied"), "pacman")

lazy val editor = Project(id = "scaled-editor", base = file("editor"))
  .dependsOn(api)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.ow2.asm"           % "asm"            % "5.0.1",
      "com.samskivert.scaled" % "mac-open-files" % "1.0",
      "de.sciss"              % "pacman"         % "0.1.0-SNAPSHOT"
    )
  )

lazy val root = Project(id = "scaled", base = file("."))
  .dependsOn(std, api, editor)
  .aggregate(std, api, editor)
  .settings(commonSettings)
  .settings(
    description := "A Scalable editor, extensible via JVM languages",
    mainClass in Compile := Some("scaled.impl.Scaled"),
    fork in run := true,
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.12" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test"
    )
  )
