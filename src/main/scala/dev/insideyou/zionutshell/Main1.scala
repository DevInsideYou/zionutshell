package dev.insideyou
package zionutshell

// Hidden dependencies

import java.io.IOException

// import zio.* // uncomment this line to switch to the real ZIO

object scope1:
  object businessLogic:
    type BusinessLogic = Has[BusinessLogic.Service]

    object BusinessLogic:
      trait Service:
        def doesGoogleHaveEvenAmountOfPicturesOf(topic: String): ZIO[Any, Nothing, Boolean]

      lazy val any: ZLayer[BusinessLogic, Nothing, BusinessLogic] =
        ZLayer.requires

      lazy val live: ZLayer[google.Google, Nothing, BusinessLogic] =
        ZLayer.fromService(make)

      def make(g: google.Google.Service): Service =
        new:
          override def doesGoogleHaveEvenAmountOfPicturesOf(
              topic: String
            ): ZIO[Any, Nothing, Boolean] =
            g.countPicturesOf(topic).map(_ % 2 == 0)

    def doesGoogleHaveEvenAmountOfPicturesOf(topic: String): ZIO[BusinessLogic, Nothing, Boolean] =
      ZIO.accessM(_.get.doesGoogleHaveEvenAmountOfPicturesOf(topic))

  object google:
    type Google = Has[Google.Service]
    object Google:
      trait Service:
        def countPicturesOf(topic: String): ZIO[Any, Nothing, Int]

      lazy val any: ZLayer[Google, Nothing, Google] =
        ZLayer.requires

    def countPicturesOf(topic: String): ZIO[Google, Nothing, Int] =
      ZIO.accessM(_.get.countPicturesOf(topic))

  object GoogleImpl:
    lazy val live: ZLayer[Any, Nothing, google.Google] =
      ZLayer.succeed(make)

    lazy val make: google.Google.Service =
      new:
        override def countPicturesOf(topic: String): ZIO[Any, Nothing, Int] =
          ZIO.succeed(if topic == "cats" then 1337 else 1338)

  object controller:
    type Controller = Has[Controller.Service]
    object Controller:
      trait Service:
        def run: ZIO[Any, IOException, Unit]

      lazy val any: ZLayer[Controller, Nothing, Controller] =
        ZLayer.requires

      lazy val live: ZLayer[businessLogic.BusinessLogic & console.Console, Nothing, Controller] =
        ZLayer.fromServices(make)

      def make(bl: businessLogic.BusinessLogic.Service, c: console.Console.Service): Service =
        new:
          override lazy val run: ZIO[Any, IOException, Unit] =
            for
              _ <- c.putStrLn("─" * 100)

              cats <- bl.doesGoogleHaveEvenAmountOfPicturesOf("cats")
              _ <- c.putStrLn(cats.toString)

              dogs <- bl.doesGoogleHaveEvenAmountOfPicturesOf("dogs")
              _ <- c.putStrLn(dogs.toString)

              _ <- c.putStrLn("─" * 100)
            yield ()

    lazy val run: ZIO[Controller, IOException, Unit] =
      ZIO.accessM(_.get.run)

  object DependencyGraph:
    lazy val env: ZLayer[Any, Nothing, controller.Controller] =
      GoogleImpl.live >>> businessLogic.BusinessLogic.live ++
        console.Console.live >>>
        controller.Controller.live

    lazy val partial: ZLayer[console.Console, Nothing, controller.Controller] =
      (GoogleImpl.live >>> businessLogic.BusinessLogic.live) ++
        console.Console.any >>>
        controller.Controller.live

  object FancyConsole:
    lazy val live: ZLayer[Any, Nothing, console.Console] =
      ZLayer.succeed(make)

    lazy val make: console.Console.Service =
      new:
        override def putStrLn(line: String): ZIO[Any, Nothing, Unit] =
          ZIO.succeed(println(scala.Console.GREEN + line + scala.Console.RESET))

        override lazy val getStrLn: ZIO[Any, Nothing, String] =
          ZIO.succeed(scala.io.StdIn.readLine())

        // For compatibility with the real ZIO
        def putStr(line: String): ZIO[Any, Nothing, Unit] =
          ZIO.succeed(print(scala.Console.GREEN + line + scala.Console.RESET))

        def putStrLnErr(line: String): ZIO[Any, Nothing, Unit] =
          ZIO.succeed(scala.Console.err.println(scala.Console.RED + line + scala.Console.RESET))

        def putStrErr(line: String): ZIO[Any, Nothing, Unit] =
          ZIO.succeed(scala.Console.err.print(scala.Console.RED + line + scala.Console.RESET))

object Main1 extends scala.App:
  Runtime.default.unsafeRunSync(program)

  import scope1.*

  object FakeBusinessLogic:
    lazy val live: ZLayer[Any, Nothing, businessLogic.BusinessLogic] =
      ZLayer.succeed(make)

    lazy val make: businessLogic.BusinessLogic.Service =
      new:
        override def doesGoogleHaveEvenAmountOfPicturesOf(
            topic: String
          ): ZIO[Any, Nothing, Boolean] =
          ZIO.succeed(false)

  lazy val fakeEnv =
    FakeBusinessLogic.live ++ console.Console.live >>> controller.Controller.live

  lazy val program =
    controller.run.provideLayer(DependencyGraph.env)

// controller.run.provideLayer(FancyConsole.live >>> DependencyGraph.partial)
// controller.run.provideSomeLayer(DependencyGraph.partial).provideLayer(FancyConsole.live)
// controller.run.provideCustomLayer(DependencyGraph.partial)
// controller.run.provideLayer(FakeBusinessLogic.live ++ DependencyGraph.env)
