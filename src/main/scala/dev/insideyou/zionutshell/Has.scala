package dev.insideyou
package zionutshell

import scala.reflect.ClassTag

final class Has[A] private (private val map: Map[String, Any])
object Has:
  def apply[A](a: A)(using tag: ClassTag[A]): Has[A] =
    new Has(Map(tag.toString -> a))

  extension [A <: Has[?]](a: A)
    inline def ++[B <: Has[?]](b: B): A & B =
      union(b)

    infix def union[B <: Has[?]](b: B): A & B =
      new Has(a.map ++ b.map).asInstanceOf[A & B]

    // Do NOT change the order of the parameter lists.
    // The current order is more type inference friendly.
    def get[S](using A => Has[S])(using tag: ClassTag[S]): S =
      a.map(tag.toString).asInstanceOf[S]
