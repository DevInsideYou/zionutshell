package dev.insideyou
package zionutshell

object Runtime:
  object default:
    def unsafeRunSync[E, A](zio: => ZIO[ZEnv, E, A]): Either[E, A] =
      zio.provideLayer(ZEnv.live).run(())
