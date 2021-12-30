val amazonSDKVersion = "1.12.129"

ThisBuild / organization := "com.frugalmechanic"
ThisBuild / licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/tpunder/sbt-s3-resolver"))
ThisBuild / publishMavenStyle := true

ThisBuild / crossScalaVersions := Vector("2.10.7", "2.12.15")
ThisBuild / crossSbtVersions := Vector("0.13.18", "1.2.8")

ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT")) {
    Some("snapshots" at nexus + "content/repositories/snapshots")
  } else {
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}

ThisBuild / scalacOptions := Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-language:implicitConversions",
  "-feature",
  "-Xlint"
) ++ (if (scalaVersion.value.startsWith("2.12")) Seq(
  // Scala 2.12 specific compiler flags
  // NOTE: These are currently broken on Scala <= 2.12.6 when using Java 9+ (will hopefully be fixed in 2.12.7)
  //"-opt:l:inline",
  //"-opt-inline-from:<sources>",
) else Nil)

// Tell the sbt-release plugin to use publishSigned
releasePublishArtifactsAction := PgpKeys.publishSigned.value

// From: https://github.com/xerial/sbt-sonatype#using-with-sbt-release-plugin
import ReleaseTransformations._

// From: https://github.com/xerial/sbt-sonatype#using-with-sbt-release-plugin
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^ test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^ publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)

ThisBuild / Test / publishArtifact := false
ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / pomExtra := (
  <developers>
    <developer>
      <id>tim</id>
      <name>Tim Underwood</name>
      <email>timunderwood@gmail.com</email>
      <url>https://github.com/tpunder</url>
    </developer>
  </developers>
  <scm>
      <connection>scm:git:git@github.com:tpunder/sbt-s3-resolver.git</connection>
      <developerConnection>scm:git:git@github.com:tpunder/sbt-s3-resolver.git</developerConnection>
      <url>git@github.com:tpunder/sbt-s3-resolver.git</url>
  </scm>)

lazy val root = project
  .in(file("."))
  .settings(
    name := "fm-sbt-s3-resolver-root",
    publish / skip := true
  )
  .aggregate(plugin, coursierHandler)

lazy val plugin = project
  .in(file("./plugin"))
  .enablePlugins(SbtPlugin, S3ScriptedPlugin)
  .settings(
    name := "fm-sbt-s3-resolver",
    description := "SBT S3 Resolver Plugin",
    scriptedBufferLog := false,
    // https://timushev.com/posts/2020/04/25/building-and-testing-sbt-plugins/
    // CI test sbt versions compatibility, but locally a single scripted command
    scriptedDependencies := Def.taskDyn {
      if (insideCI.value) Def.task(())
      else Def.task(())
        .dependsOn(publishLocal)
        .dependsOn(coursierHandler / publishLocal)
    }.value,
    sbtVersion := (LocalRootProject / pluginCrossBuild / sbtVersion ).value,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value,
      //      "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1044",
      //      "-Xdebug"
    )
  )
  .dependsOn(coursierHandler)

lazy val coursierHandler = project
  .in(file("./coursier-handler"))
  .enablePlugins(SbtPlugin)
  .disablePlugins(S3ScriptedPlugin)
  .settings(
    name := "fm-sbt-s3-resolver-coursier-handler",
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk-s3" % amazonSDKVersion,
      "com.amazonaws" % "aws-java-sdk-sts" % amazonSDKVersion,
      "org.apache.ivy" % "ivy" % "2.5.0",
      "org.scalatest" %% "scalatest" % "3.2.10" % Test
    ),
    sbtVersion := (LocalRootProject / pluginCrossBuild / sbtVersion ).value,
    // additional custom protocol support added in 2.0.9 (https://github.com/coursier/sbt-coursier/commit/92e40c22256bea44d1e1befbef1cb2a627f8b155)
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 12 =>
          Seq(
            "io.get-coursier" %% "lm-coursier" % "2.0.10-1",
            "io.get-coursier" %% "lm-coursier-shaded" % "2.0.10-1",
          )
        case _ => Nil
      }
    }
  )