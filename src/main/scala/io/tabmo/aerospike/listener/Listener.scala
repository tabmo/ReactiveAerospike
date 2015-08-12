package io.tabmo.aerospike.listener

import scala.concurrent.Future

import com.aerospike.client.listener._
import com.aerospike.client.{AerospikeException, Key, Record}

import io.tabmo.aerospike.data._

case class AerospikeWriteListener[T](implicit keyConverter: AerospikeKeyConverter[T]) extends Listener[AerospikeKey[T]] with WriteListener {
  override def onSuccess(key: Key) = promise.success(AerospikeKey[T](key))
  override def onFailure(exception: AerospikeException) = promise.failure(new AerospikeException(exception))
}

case class AerospikeReadListener() extends Listener[AerospikeRecord] with RecordListener {
  override def onSuccess(key: Key, record: Record) = promise.success(AerospikeRecord(record))
  override def onFailure(exception: AerospikeException) = promise.failure(new AerospikeException(exception))
}

case class AerospikeOptionalReadListener() extends Listener[Option[AerospikeRecord]] with RecordListener {
  override def onSuccess(key: Key, record: Record) = promise.success(AerospikeRecord.optional(record))
  override def onFailure(exception: AerospikeException) = promise.failure(new AerospikeException(exception))
}

case class AerospikeReadSequenceListener[T](implicit keyConverter: AerospikeKeyConverter[T]) extends Listener[Map[AerospikeKey[T], AerospikeRecord]] with RecordSequenceListener {
  private val stream = Map.newBuilder[AerospikeKey[T], AerospikeRecord]
  override def onRecord(key: Key, record: Record) = stream += ((AerospikeKey(key), AerospikeRecord(record)))
  override def onSuccess() = promise.success(stream.result())
  override def onFailure(exception: AerospikeException) = promise.failure(new AerospikeException(exception))
}

case class AerospikeDeleteListener() extends Listener[Boolean] with DeleteListener {
  override def onSuccess(key: Key, existed: Boolean) = promise.success(existed)
  override def onFailure(exception: AerospikeException) = promise.failure(new AerospikeException(exception))
}

case class AerospikeExistsListener() extends Listener[Boolean] with ExistsListener {
  override def onSuccess(key: Key, exists: Boolean) = promise.success(exists)
  override def onFailure(exception: AerospikeException) = promise.failure(new AerospikeException(exception))
}

trait Listener[T] {
  protected val promise = scala.concurrent.Promise[T]()
  val result: Future[T] = promise.future
}
