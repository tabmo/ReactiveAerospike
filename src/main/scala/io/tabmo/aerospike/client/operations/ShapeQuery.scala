package io.tabmo.aerospike.client.operations

import scala.concurrent.{ExecutionContext, Future, Promise}
import java.util

import com.aerospike.client.listener.RecordListener
import com.aerospike.client.{AerospikeException, Key, Record, Value}
import io.tabmo.aerospike.TSafe.VRestriction
import io.tabmo.aerospike.client.ReactiveAerospikeClient
import io.tabmo.aerospike.data.{AerospikeKeyConverter, AerospikeRecord}
import io.tabmo.aerospike.utils.{BinShape, Shape}

import shapeless.HList

trait ShapeQuery {

  protected def prepareQueryEquals[L <: HList, LR <: HList, T, V: VRestriction](shape: Shape[L, LR, T], filterBin: BinShape[V])(implicit keyConverter: AerospikeKeyConverter[String]): PreparedEqualQuery[T, V] = {
    new PreparedEqualQuery[T, V] {

      override def apply(value: V)(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): Future[Seq[T]] = {
        client.queryEqual(shape.ns, shape.set, shape.binNames(), filterBin.name, value).map {
          _.values.map { r =>
            shape.unshape(r)
          }(collection.breakOut)
        }
      }
    }
  }

  protected def prepareQueryRange[L <: HList, LR <: HList, T](shape: Shape[L, LR, T], filterBin: BinShape[Long])(implicit keyConverter: AerospikeKeyConverter[String]): PreparedRangeQuery[T] = {
    new PreparedRangeQuery[T] {

      override def apply(min: Long, max: Long)(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): Future[Seq[T]] = {
        client.queryRange(shape.ns, shape.set, shape.binNames(), filterBin.name, min, max).map {
          _.values.map { r =>
            shape.unshape(r)
          }.toSeq
        }
      }
    }
  }

  protected def prepareQueryIndex[L <: HList, LR <: HList, T](shape: Shape[L, LR, T]) = {
    val binsName = shape.binNames()
    new PreparedIndexQuery[T] {
      override def apply[V: VRestriction](k: V)(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): Future[Option[T]] = {

        val key = k match {
          case v: Long => new Key(shape.ns, shape.set, v)
          case v: Int => new Key(shape.ns, shape.set, v)
          case v: String => new Key(shape.ns, shape.set, v)
        }

        val promise = Promise[Option[T]]()
        client.asyncClient.get(null, new RecordListener {
          override def onFailure(exception: AerospikeException): Unit = promise.failure(exception)

          override def onSuccess(key: Key, record: Record): Unit = {
            if (record == null) promise.success(None)
            else promise.success(Some(shape.unshape(AerospikeRecord(record))))
          }
        }, key, binsName: _*)
        promise.future
      }
    }
  }

  protected def prepareQueryAggregateEquals[L <: HList, LR <: HList, T, V: VRestriction](module: String, function: String, shape: Shape[L, LR, T], filterBin: BinShape[V]): PreparedAggregateQueryEqual[T, V] = {
    new PreparedAggregateQueryEqual[T, V] {
      override def apply(filterValue: V, values: Value*)(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): Future[Seq[T]] = {
        client.queryEqualAggregate(shape.ns, shape.set, filterBin.name, filterValue, this.getClass.getClassLoader, null, module, function, values).map {
          _.map { r =>
            shape.unshape(r)
          }
        }
      }
    }
  }

  protected def prepareQueryAggregateRange[L <: HList, LR <: HList, T](module: String, function: String, shape: Shape[L, LR, T], filterBin: BinShape[Long]): PreparedAggregateQueryRange[T] = {
    new PreparedAggregateQueryRange[T] {
      override def apply(min: Long, max: Long, values: Value*)(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): Future[Seq[T]] = {
        client.queryRangeAggregate(shape.ns, shape.set, filterBin.name, min, max, this.getClass.getClassLoader, null, module, function, values).map {
          _.map { r =>
            shape.unshape(r)
          }
        }
      }
    }
  }
}

trait PreparedEqualQuery[T, V] {
  def apply(value: V)(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): Future[Seq[T]]
}

trait PreparedRangeQuery[T] {
  def apply(min: Long, max: Long)(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): Future[Seq[T]]
}

trait PreparedIndexQuery[T] {
  def apply[V: VRestriction](key: V)(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): Future[Option[T]]
}

trait PreparedAggregateQueryEqual[T, V] {
  def apply(filterValue: V, values: Value*)(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): Future[Seq[T]]
}

trait PreparedAggregateQueryRange[T] {
  def apply(min: Long, max: Long, values: Value*)(implicit ctx: ExecutionContext, client: ReactiveAerospikeClient): Future[Seq[T]]
}