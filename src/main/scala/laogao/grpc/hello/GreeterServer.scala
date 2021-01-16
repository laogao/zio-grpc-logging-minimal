package laogao.grpc.hello

import io.grpc.Status
import zio.{Has, IO, ZEnv, ZIO}
import laogao.grpc.hello.ZioHello.ZGreeter
import scalapb.zio_grpc.RequestContext
import zio.logging.{LogFormat, LogLevel, Logging, log}
import scalapb.zio_grpc.ServerLayer
import io.grpc.ServerBuilder

case class User(id: Long, name: String)

object GreeterImpl extends ZGreeter[ZEnv with Logging, Has[User]] {

  val logging =
    Logging.console(
      logLevel = LogLevel.Debug,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("zio-grpc-demo")

  def sayHello(request: HelloRequest): ZIO[ZEnv with Has[User] with Logging, Status, HelloReply] =
    for {
      _ <- log.info(s"Got request: $request")
      user <- ZIO.service[User]
    } yield HelloReply(s"Hello, ${if (user.id > 0) user.name else request.name}!")

}

import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList

object GreeterServer extends zio.App {
  val USER_KEY = io.grpc.Metadata.Key.of("user-token", io.grpc.Metadata.ASCII_STRING_MARSHALLER)

  def findUser(rc: RequestContext): IO[Status, User] =
    rc.metadata.get(USER_KEY).flatMap {
      case Some(name) => IO.succeed(User(System.currentTimeMillis(), name))
      case _          => IO.fail(Status.UNAUTHENTICATED.withDescription("No access!"))
    }

  val serverLive = ServerLayer.fromServiceLayer(ServerBuilder.forPort(9000))(
    GreeterImpl.transformContextM(findUser).toLayer)

  val env = Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("my-component")

  val ourApp = (ZEnv.live ++ env) >>> serverLive

  def run(args: List[String]) = ourApp.build.useForever.exitCode
}