package laogao.grpc

import laogao.grpc.hello.ZioHello.GreeterClient
import laogao.grpc.hello.GreeterServer
import laogao.grpc.hello.HelloRequest
import io.grpc.{ManagedChannelBuilder, Metadata}
import zio.console._
import scalapb.zio_grpc.{SafeMetadata, ZManagedChannel}
import zio.logging.{LogFormat, LogLevel, Logging, log}

object Main extends zio.App {

  val channel: ManagedChannelBuilder[_] = ManagedChannelBuilder.forAddress("localhost", 9000).usePlaintext()
  val clientManaged = GreeterClient.managed(ZManagedChannel(channel))

  val logging =
    Logging.console(
      logLevel = LogLevel.Debug,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("zio-grpc-demo")

  def myAppLogic = for {
    r1 <- clientManaged.use(client => client
      .withTimeoutMillis(1000)
      .withMetadataM(SafeMetadata.fromMetadata {
        val headers = new Metadata()
        headers.put(Metadata.Key.of("user-token", Metadata.ASCII_STRING_MARSHALLER), "Aegon")
        headers
      })
      .sayHello(HelloRequest("Jon"))
      .mapError(_.asRuntimeException()))
    _ <- log.debug(s"gRPC call has returned: ${r1.message}")
    _ <- log.debug("waiting for user input...") *> putStrLn("Press enter to exit...") *> getStrLn
  } yield ()

  // by providing GreeterServer this way we don't have to manually (re)start it for quick demo
  final def run(args: List[String]) =
    myAppLogic.provideCustomLayer(logging ++ GreeterServer.ourApp).exitCode

}