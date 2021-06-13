package dev.insideyou
package zionutshell

// Effect tracking

import java.io.IOException

// import zio.* // uncomment this line to switch to the real ZIO

object scope2:
  object businessLogic:
    type BusinessLogic = Has[BusinessLogic.Service]

    object BusinessLogic:
      trait Service:
        def doesGoogleHaveEvenAmountOfPicturesOf(
            topic: String
          ): ZIO[google.Google, Nothing, Boolean]

      // Mostly unnecessary with this "effect tracking" pattern
      lazy val any: ZLayer[BusinessLogic, Nothing, BusinessLogic] =
        ZLayer.requires

      lazy val live: ZLayer[Any, Nothing, BusinessLogic] =
        ZLayer.succeed(make)

      lazy val make: Service =
        new:
          override def doesGoogleHaveEvenAmountOfPicturesOf(
              topic: String
            ): ZIO[google.Google, Nothing, Boolean] =
            google.countPicturesOf(topic).map(_ % 2 == 0)

    def doesGoogleHaveEvenAmountOfPicturesOf(
        topic: String
      ): ZIO[BusinessLogic & google.Google, Nothing, Boolean] =
      ZIO.accessM(_.get.doesGoogleHaveEvenAmountOfPicturesOf(topic))

  object google:
    type Google = Has[Google.Service]
    object Google:
      trait Service:
        def countPicturesOf(topic: String): ZIO[Any, Nothing, Int]

    // Mostly unnecessary with this "effect tracking" pattern
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
        def run
            : ZIO[businessLogic.BusinessLogic & google.Google & console.Console, IOException, Unit]

      // Mostly unnecessary with this "effect tracking" pattern
      lazy val any: ZLayer[Controller, Nothing, Controller] =
        ZLayer.requires

      lazy val live: ZLayer[Any, Nothing, Controller] =
        ZLayer.succeed(make)

      lazy val make: Service =
        new:
          override lazy val run
              : ZIO[businessLogic.BusinessLogic & google.Google & console.Console, IOException, Unit] =
            for
              _ <- console.putStrLn("─" * 100)

              cats <- businessLogic.doesGoogleHaveEvenAmountOfPicturesOf("cats")
              _ <- console.putStrLn(cats.toString)

              dogs <- businessLogic.doesGoogleHaveEvenAmountOfPicturesOf("dogs")
              _ <- console.putStrLn(dogs.toString)

              _ <- console.putStrLn("─" * 100)
            yield ()

    lazy val run
        : ZIO[Controller & businessLogic.BusinessLogic & google.Google & console.Console, IOException, Unit] =
      ZIO.accessM(_.get.run)

  object DependencyGraph:
    lazy val env =
      GoogleImpl.live ++ businessLogic.BusinessLogic.live ++ controller.Controller.live ++
        console.Console.live

    // you could also append this line but it is unnecessary
    // ++ console.Console.any
    lazy val partial =
      GoogleImpl.live ++ businessLogic.BusinessLogic.live ++ controller.Controller.live

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

object Main2 extends scala.App:
  Runtime.default.unsafeRunSync(program)

  import scope2.*

  object FakeBusinessLogic:
    lazy val live: ZLayer[Any, Nothing, businessLogic.BusinessLogic] =
      ZLayer.succeed(make)

    lazy val make: businessLogic.BusinessLogic.Service =
      new:
        override def doesGoogleHaveEvenAmountOfPicturesOf(
            topic: String
          ): ZIO[google.Google, Nothing, Boolean] =
          ZIO.succeed(true)

  lazy val program =
    controller.run.provideLayer(DependencyGraph.env)
// controller.run.provideLayer(DependencyGraph.env ++ FakeBusinessLogic.live)
// controller.run.provideLayer(FancyConsole.live ++ DependencyGraph.partial)
// controller.run.provideLayer(DependencyGraph.partial ++ FancyConsole.live)
// controller.run.provideLayer(DependencyGraph.env ++ FancyConsole.live)
// controller.run.provideLayer(FancyConsole.live ++ DependencyGraph.env)

// controller
//   .run
//   .provideSomeLayer[console.Console](DependencyGraph.partial)
//   .provideLayer(FancyConsole.live)

// controller.run.provideCustomLayer(DependencyGraph.partial)
