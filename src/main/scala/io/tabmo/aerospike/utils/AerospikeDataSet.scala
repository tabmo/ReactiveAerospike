package io.tabmo.aerospike.utils

import io.tabmo.aerospike.data.AerospikeRecord

import cats.Functor
import shapeless.ops.hlist.{RightFolder, _}
import shapeless.{HList, HNil}

abstract class AerospikeDataSet(val namespace: String, val set: String) {

  protected def long(k: String) = ShapeReader.long(k)

  protected def longTo[A](k: String)(implicit f: Long => A) = long(k).map(f)

  protected def optionLong(k: String)(implicit functor: Functor[Option]) = ShapeReader.optionLong(k)

  protected def optionLongTo[A](k: String)(implicit functor: Functor[Option], f: Long => A) = optionLong(k).map(f)

  protected def string(k: String) = ShapeReader.string(k)

  protected def stringTo[A](k: String)(implicit f: String => A) = string(k).map(f)

  protected def optionString(k: String)(implicit functor: Functor[Option]) = ShapeReader.optionString(k)

  protected def optionStringTo[A](k: String)(implicit functor: Functor[Option], f: String => A) = optionString(k).map(f)

  protected def shaped[L <: HList, LR <: HList](bins: L)(implicit folder: RightFolder.Aux[L, (AerospikeRecord, HNil), extractData.type, (AerospikeRecord, LR)], ev: ToList[L, BinShape[_]]): DefaultShape[L, LR] = DefaultShape[L, LR](namespace, set, bins)
}
