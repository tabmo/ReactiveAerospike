package io.tabmo.aerospike.utils

import cats.Functor

trait BinShape[T] {
  val name: String
}

case class ShapeReader[F[_], A, B](name: String, fun: A => B)(implicit functor: Functor[F]) extends BinShape[A] {

  def map[C](implicit f: B => C): ShapeReader[F, A, C] = {
    ShapeReader(name, f.compose(fun))
  }

  def read(source: F[A]): F[B] = {
    functor.lift(fun)(source)
  }

}

object ShapeReader {
  def long(name: String): LongShape[Long] = ShapeReader[cats.Id, Long, Long](name, identity[Long])

  def string(name: String): StringShape[String] = ShapeReader[cats.Id, String, String](name, identity[String])

  def optionLong(name: String)(implicit functor: Functor[Option]): OptionLongShape[Long] = ShapeReader[Option, Long, Long](name, identity[Long])(functor)

  def optionString(name: String)(implicit functor: Functor[Option]): OptionStringShape[String] = ShapeReader[Option, String, String](name, identity[String])(functor)
}


