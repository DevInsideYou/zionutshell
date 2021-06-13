package dev.insideyou
package zionutshell

import java.io.IOException

object console:
  type Console = Has[Console.Service]
  object Console:
    trait Service:
      def putStrLn(line: String): ZIO[Any, IOException, Unit]
      def getStrLn: ZIO[Any, IOException, String]

    lazy val any: ZLayer[Console, Nothing, Console] =
      ZLayer.requires

    lazy val live: ZLayer[Any, Nothing, Console] =
      ZLayer.succeed(make)

    lazy val make: Service =
      new:
        def putStrLn(line: String): ZIO[Any, IOException, Unit] =
          ZIO.succeed(println(line))

        lazy val getStrLn: ZIO[Any, IOException, String] =
          ZIO.succeed(scala.io.StdIn.readLine())

  def putStrLn(line: => String): ZIO[Console, IOException, Unit] =
    ZIO.accessM(_.get.putStrLn(line))

  def getStrLn: ZIO[Console, IOException, String] =
    ZIO.accessM(_.get.getStrLn)
