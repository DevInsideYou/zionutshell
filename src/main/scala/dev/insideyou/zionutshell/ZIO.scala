package dev.insideyou
package zionutshell

import scala.reflect.ClassTag

final class ZIO[-R, +E, +A](val run: R => Either[E, A]):
  def flatMap[R1 <: R, E1 >: E, B](azb: A => ZIO[R1, E1, B]): ZIO[R1, E1, B] =
    ZIO(r => run(r).fold(ZIO.fail, azb).run(r))

  def zip[R1 <: R, E1 >: E, B](that: ZIO[R1, E1, B]): ZIO[R1, E1, (A, B)] =
    for
      a <- this
      b <- that
    yield a -> b

  def map[B](ab: A => B): ZIO[R, E, B] =
    ZIO(r => run(r).map(ab))

  def catchAll[R1 <: R, E2, A1 >: A](h: E => ZIO[R1, E2, A1]): ZIO[R1, E2, A1] =
    ZIO(r => run(r).fold(h, ZIO.succeed).run(r))

  def mapError[E2](h: E => E2): ZIO[R, E2, A] =
    ZIO(r => run(r).left.map(h))

  def provideCustomLayer[E1 >: E, B <: Has[?]](
      layer: ZLayer[ZEnv, E1, B]
    )(using
      ZEnv & B => R
    ): ZIO[ZEnv, E1, A] =
    provideSomeLayer(layer)

  def provideSomeLayer[R0 <: Has[?]]: ProvideSomeLayer[R0] =
    ProvideSomeLayer[R0]

  final class ProvideSomeLayer[R0 <: Has[?]]:
    def apply[E1 >: E, B <: Has[?]](layer: ZLayer[R0, E1, B])(using R0 & B => R): ZIO[R0, E1, A] =
      provideLayer(ZLayer.identity[R0] ++ layer)

  def provideLayer[R1]: ProvideLayer[R1] =
    ProvideLayer[R1]

  final class ProvideLayer[R1]:
    def apply[E1 >: E, B](layer: ZLayer[R1, E1, B])(using view: B => R): ZIO[R1, E1, A] =
      layer.zio.map(view).flatMap(r => provide(r))

  def provideSome[R0](f: R0 => R): ZIO[R0, E, A] =
    ZIO.accessM(r0 => provide(f(r0)))

  def provide(r: => R): ZIO[Any, E, A] =
    ZIO(_ => run(r))

object ZIO:
  def succeed[A](a: => A): ZIO[Any, Nothing, A] =
    ZIO(r => Right(a))

  def fail[E](e: => E): ZIO[Any, E, Nothing] =
    ZIO(r => Left(e))

  def effect[A](a: => A): ZIO[Any, Throwable, A] =
    ZIO { r =>
      try Right(a)
      catch Left(_)
    }

  def fromFunction[R, A](run: R => A): ZIO[R, Nothing, A] =
    ZIO(r => Right(run(r)))

  def access[R]: AccessPartiallyApplied[R] =
    AccessPartiallyApplied[R]

  final class AccessPartiallyApplied[R]:
    def apply[A](f: R => A): ZIO[R, Nothing, A] =
      ZIO.environment.map(f)

  def accessM[R]: AccessMPartiallyApplied[R] =
    AccessMPartiallyApplied[R]

  final class AccessMPartiallyApplied[R]:
    def apply[E, A](f: R => ZIO[R, E, A]): ZIO[R, E, A] =
      ZIO.environment.flatMap(f)

  inline def environment[R]: ZIO[R, Nothing, R] =
    identity

  def identity[R]: ZIO[R, Nothing, R] =
    ZIO.fromFunction(Predef.identity)
