addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2-1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.6")

libraryDependencies ++= Seq(
  "com.dimafeng" %% "testcontainers-scala-localstack" % "0.39.12",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.129",
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)