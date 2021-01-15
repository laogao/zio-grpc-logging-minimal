ThisBuild / scalaVersion := "2.13.4"
ThisBuild / organization := "laogao"
ThisBuild / version      := "1.0"

val ZIOVersion        = "1.0.3"
val ZIOLoggingVersion = "0.5.4"
val ZIOInteropVersion = "2.2.0.1"
val grpcVersion       = "1.34.1"
val zioGrpcVersion    = "0.4.2"

lazy val grpc = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(ProtocPlugin)
  .settings(
    name := "grpc",
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true) -> (sourceManaged in Compile).value,
      scalapb.zio_grpc.ZioCodeGenerator -> (sourceManaged in Compile).value
    ),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-streams" % ZIOVersion,
      "dev.zio" %% "zio-logging" % ZIOLoggingVersion,
      "io.grpc" % "grpc-netty" % grpcVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-json4s" % "0.10.1",
      "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-core" % zioGrpcVersion
    )
  )
