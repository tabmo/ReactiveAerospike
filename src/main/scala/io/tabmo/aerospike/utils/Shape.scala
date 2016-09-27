package io.tabmo.aerospike.utils

import io.tabmo.aerospike.data.AerospikeRecord

import shapeless.ops.hlist._
import shapeless.{Generic, HList, HNil, Poly}

abstract class Shape[L <: HList, LR <: HList, T]()(implicit folder: RightFolder.Aux[L, (AerospikeRecord, HNil), extractData.type, (AerospikeRecord, LR)], ev: ToList[L, BinShape[_]]){
  val ns: String
  val set: String
  val bins: L
  def unshape(record: AerospikeRecord): T = mapper(bins.foldRight(record, HNil: HNil)(extractData)._2)
  def binNames() = bins.toList.map(_.name)
  protected def mapper(list: LR): T
}

case class DefaultShape[L <: HList, LR <: HList](ns: String, set: String, bins: L)(implicit folder: RightFolder.Aux[L, (AerospikeRecord, HNil), extractData.type, (AerospikeRecord, LR)], ev: ToList[L, BinShape[_]]) extends Shape[L,LR,LR] {
  override protected def mapper(list: LR): LR = list
  def to[T]()(implicit gen: Generic.Aux[T,LR]) = new GenericShape[L,LR, T](ns,set,bins)
  def tupled[T]()(implicit tupler: Tupler.Aux[LR,T]) = new TupleShape[L,LR, T](ns,set,bins)
}

case class GenericShape[L <: HList, LR <: HList, T](ns: String, set: String, bins: L)(implicit folder: RightFolder.Aux[L, (AerospikeRecord, HNil), extractData.type, (AerospikeRecord, LR)], ev: ToList[L, BinShape[_]], gen: Generic.Aux[T,LR]) extends Shape[L,LR,T] {
  override protected def mapper(list: LR): T = gen.from(list)
}

case class TupleShape[L <: HList, LR <: HList, T](ns: String, set: String, bins: L)(implicit folder: RightFolder.Aux[L, (AerospikeRecord, HNil), extractData.type, (AerospikeRecord, LR)], ev: ToList[L, BinShape[_]], tupler: Tupler.Aux[LR,T]) extends Shape[L,LR,T] {
  override protected def mapper(list: LR): T = list.tupled
}

object extractData extends Poly {
  implicit def caseBinString[L <: HList, T] = use((b: StringShape[T], t: (AerospikeRecord, L)) => (t._1, b.read(t._1.getString(b.name)) :: t._2))

  implicit def caseBinOptString[L <: HList, T] = use((b: OptionStringShape[T], t: (AerospikeRecord, L)) => (t._1, b.read(t._1.getOptString(b.name)) :: t._2))

  implicit def caseBinLong[L <: HList, T] = use((b: LongShape[T], t: (AerospikeRecord, L)) => (t._1, b.read(t._1.getLong(b.name)) :: t._2))

  implicit def caseBinOptLong[L <: HList, T] = use((b: OptionLongShape[T], t: (AerospikeRecord, L)) => (t._1, b.read(t._1.getOptLong(b.name)) :: t._2))
}


