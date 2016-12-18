package io.tabmo.aerospike.validation

import jto.validation.aerospike._

trait AsEncoder[A] {
  def encode(a: A): AsValue
}

object AsEncoder {
  import shapeless._
  import shapeless.labelled._
  import shapeless.ops.hlist.IsHCons

  def instance[A](f: A => AsValue) = new AsEncoder[A] {
    override def encode(a: A): AsValue = f(a)
  }

  implicit val longEncoder: AsEncoder[Long] = instance(AsLong)

  implicit val stringEncoder: AsEncoder[String] = instance(AsString)

  implicit def listEncoder[A](implicit ev: AsEncoder[A]): AsEncoder[List[A]] = instance { list =>
    AsArray(list.map(ev.encode).toIndexedSeq)
  }

  implicit def optionEncoder[A](implicit ev: AsEncoder[A]): AsEncoder[Option[A]] = instance {
    case Some(a) => ev.encode(a)
    case None => AsNull
  }

  implicit val hnilEncoder: AsEncoder[HNil] = instance(_ => AsObject())

  implicit def hlistEncoder[K <: Symbol, H, T <: shapeless.HList](
    implicit witness: Witness.Aux[K],
    isHCons: IsHCons.Aux[H :: T, H, T],
    hEncoder: Lazy[AsEncoder[H]],
    tEncoder: Lazy[AsEncoder[T]]
  ): AsEncoder[FieldType[K, H] :: T] = instance { o =>
    val head = AsObject(witness.value.name, hEncoder.value.encode(isHCons.head(o)))
    val tail = tEncoder.value.encode(isHCons.tail(o))
    head ++ tail.asInstanceOf[AsObject]
  }

  implicit def objectEncoder[A, Repr <: HList](
    implicit gen: LabelledGeneric.Aux[A, Repr],
    hlistEncoder: AsEncoder[Repr]
  ): AsEncoder[A] = instance { o =>
    hlistEncoder.encode(gen.to(o)).asInstanceOf[AsObject]
  }

  def apply[A](implicit ev: AsEncoder[A]): AsEncoder[A] = ev
}
