package dev.insideyou
package zionutshell

import scala.reflect.ClassTag

final class ZLayer[-R, +E, +A](val zio: ZIO[R, E, A]):
  inline def flatMap[R1 <: R, E1 >: E, B](azb: A => ZLayer[R1, E1, B]): ZLayer[R1, E1, B] =
    ZLayer(this.zio.flatMap(a => azb(a).zio))

  inline def zip[R1 <: R, E1 >: E, B](that: ZLayer[R1, E1, B]): ZLayer[R1, E1, (A, B)] =
    ZLayer(this.zio.zip(that.zio))

  inline def map[B](ab: A => B): ZLayer[R, E, B] =
    ZLayer(this.zio.map(ab))

  inline def provideSome[R0](f: R0 => R): ZLayer[R0, E, A] =
    ZLayer(this.zio.provideSome(f))

  inline def provide(r: => R): ZLayer[Any, E, A] =
    ZLayer(this.zio.provide(r))

  def >>>[E1 >: E, B <: Has[?]](
      that: ZLayer[A, E1, B]
    )(using
      A => Has[?]
    ): ZLayer[R, E1, B] =
    this.flatMap(a => that.provide(a))

  def ++[R1 <: Has[?], E1 >: E, B <: Has[?]](
      that: ZLayer[R1, E1, B]
    )(using
      view: A => Has[?]
    ): ZLayer[R & R1, E1, A & B] =
    this.zip(that).map((a, b) => view(a).union(b).asInstanceOf[A & B])

object ZLayer:
  def succeed[A: ClassTag](
      a: => A
    ): ZLayer[Any, Nothing, Has[A]] =
    ZLayer(ZIO.succeed(Has(a)))

  def fromService[R <: Has[S], S: ClassTag, A: ClassTag](
      f: S => A
    ): ZLayer[R, Nothing, Has[A]] =
    ZLayer(ZIO.fromFunction(r => Has(f(r.get[S]))))

  def fromServices[R <: Has[S1] & Has[S2], S1: ClassTag, S2: ClassTag, A: ClassTag](
      f: (S1, S2) => A
    ): ZLayer[R, Nothing, Has[A]] =
    ZLayer(ZIO.fromFunction(r => Has(f(r.get[S1], r.get[S2]))))

  inline def requires[R]: ZLayer[R, Nothing, R] =
    identity[R]

  def identity[R]: ZLayer[R, Nothing, R] =
    ZLayer(ZIO.identity)
