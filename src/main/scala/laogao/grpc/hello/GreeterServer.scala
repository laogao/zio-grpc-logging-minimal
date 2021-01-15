package laogao.grpc.hello

import io.grpc.Status
import zio.{Has, IO, ZEnv, ZIO}
import laogao.grpc.hello.ZioHello.ZGreeter
import scalapb.zio_grpc.RequestContext
import zio.logging.{LogFormat, LogLevel, Logging, log}

case class User(id: Long, name: String)

object GreeterImpl extends ZGreeter[ZEnv, Has[User]] {

  val logging =
    Logging.console(
      logLevel = LogLevel.Debug,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("zio-grpc-demo")

  def sayHello(request: HelloRequest): ZIO[ZEnv with Has[User], Status, HelloReply] = {
    val exec = for {
      //_ <- log.info(s"Got request: $request")
      user <- ZIO.service[User]
    } yield HelloReply(s"Hello, ${if (user.id > 0) user.name else request.name}!")
    exec//.provideSomeLayer(logging)
  }

}

import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList

object GreeterServer extends ServerMain {

  val USER_KEY = io.grpc.Metadata.Key.of("user-token", io.grpc.Metadata.ASCII_STRING_MARSHALLER)

  def findUser(rc: RequestContext): IO[Status, User] =
    rc.metadata.get(USER_KEY).flatMap {
      case Some(name) => IO.succeed(User(System.currentTimeMillis(), name))
      case _          => IO.fail(Status.UNAUTHENTICATED.withDescription("No access!"))
    }

  def services: ServiceList[zio.ZEnv] = ServiceList.add(GreeterImpl.transformContextM(findUser))

}