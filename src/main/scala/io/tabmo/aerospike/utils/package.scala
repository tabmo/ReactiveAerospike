package io.tabmo.aerospike

import scala.concurrent.ExecutionContext

import io.tabmo.aerospike.TSafe.VRestriction
import io.tabmo.aerospike.client.ReactiveAerospikeClient
import io.tabmo.aerospike.client.operations._
import io.tabmo.aerospike.data.AerospikeKeyConverter

import shapeless.HList

package object utils {

  implicit class QueryingShape[L <: HList, LR <: HList, T](shape: Shape[L, LR, T]) extends ShapeQuery {

    def prepareQueryEquals[V: VRestriction](filterBin: BinShape[V])(implicit keyConverter: AerospikeKeyConverter[String]): PreparedEqualQuery[T, V] = prepareQueryEquals(shape, filterBin)

    def prepareQueryRange(filterBin: BinShape[Long])(implicit keyConverter: AerospikeKeyConverter[String]): PreparedRangeQuery[T] = prepareQueryRange(shape, filterBin)

    def prepareQueryIndex[V]()(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): PreparedIndexQuery[T] = prepareQueryIndex[L, LR, T](shape)

    def prepareQueryAggregateEquals[V: VRestriction](module: String, function: String, filterBin: BinShape[V]): PreparedAggregateQueryEqual[T, V] = prepareQueryAggregateEquals(module, function, shape, filterBin)

    def prepareQueryAggregateRange(module: String, function: String, filterBin: BinShape[Long]): PreparedAggregateQueryRange[T] = prepareQueryAggregateRange(module, function, shape, filterBin)
  }

  type LongShape[T] = ShapeReader[cats.Id, Long, T]
  type OptionLongShape[T] = ShapeReader[Option, Long, T]
  type StringShape[T] = ShapeReader[cats.Id, String, T]
  type OptionStringShape[T] = ShapeReader[Option, String, T]
}
