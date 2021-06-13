package dev.insideyou
package zionutshell

type ZEnv = console.Console // ++ Clock ++ System ++ Random ++ Blocking
object ZEnv:
  val any: ZLayer[ZEnv, Nothing, ZEnv] =
    ZLayer.requires

  val live: ZLayer[Any, Nothing, ZEnv] =
    console.Console.live // ++ Clock.live ++ System.live ++ Random.live ++ Blocking.live
