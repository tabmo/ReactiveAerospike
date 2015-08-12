package io.tabmo.aerospike.client.operations

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import com.aerospike.client.Bin
import com.aerospike.client.listener.RecordListener
import com.aerospike.client.policy.{BatchPolicy, Policy, WritePolicy}

import io.tabmo.aerospike.client.ReactiveAerospikeClient
import io.tabmo.aerospike.data.{AerospikeKey, AerospikeKeyConverter, AerospikeRecord}
import io.tabmo.aerospike.listener._

trait BasicOperations {
  self: ReactiveAerospikeClient =>

  def put[K](key: AerospikeKey[K], bins: Seq[Bin], policy: Option[WritePolicy] = None)
    (implicit keyConverter: AerospikeKeyConverter[K]): Future[AerospikeKey[K]] = {

    logger.debug(s"PUT {$key} ${bins.mkString(", ")}")

    val listener = AerospikeWriteListener[K]()
    asyncClient.put(policy.orNull, listener, key.inner, bins: _*)
    listener.result
  }

  def append[K](key: AerospikeKey[K], bins: Seq[Bin], policy: Option[WritePolicy] = None)
    (implicit keyConverter: AerospikeKeyConverter[K]): Future[AerospikeKey[K]] = {

    logger.debug(s"APPEND {$key} ${bins.mkString(", ")}")

    val listener = AerospikeWriteListener[K]()
    asyncClient.append(policy.orNull, listener, key.inner, bins: _*)
    listener.result
  }

  def prepend[K](key: AerospikeKey[K], bins: Seq[Bin], policy: Option[WritePolicy] = None)
    (implicit keyConverter: AerospikeKeyConverter[K]): Future[AerospikeKey[K]] = {

    logger.debug(s"PREPEND {$key} ${bins.mkString(", ")}")

    val listener = AerospikeWriteListener[K]()
    asyncClient.prepend(policy.orNull, listener, key.inner, bins: _*)
    listener.result
  }

  def add[K](key: AerospikeKey[K], bins: Seq[Bin], policy: Option[WritePolicy] = None)
    (implicit keyConverter: AerospikeKeyConverter[K]): Future[AerospikeKey[K]] = {

    logger.debug(s"ADD {$key} ${bins.mkString(", ")}")

    val listener = AerospikeWriteListener[K]()
    asyncClient.add(policy.orNull, listener, key.inner, bins: _*)
    listener.result
  }

  def delete(key: AerospikeKey[_], policy: Option[WritePolicy] = None): Future[Boolean] = {

    logger.debug(s"DELETE {$key}")

    val listener = AerospikeDeleteListener()
    asyncClient.delete(policy.orNull, listener, key.inner)
    listener.result
  }

  def touch[K](key: AerospikeKey[K], policy: Option[WritePolicy] = None)
    (implicit keyConverter: AerospikeKeyConverter[K]): Future[AerospikeKey[K]] = {

    logger.debug(s"TOUCH {$key}")

    val listener = AerospikeWriteListener[K]()
    asyncClient.touch(policy.orNull, listener, key.inner)
    listener.result
  }

  def exists(key: AerospikeKey[_], policy: Option[WritePolicy] = None): Future[Boolean] = {

    logger.debug(s"EXISTS {$key}")

    val el = AerospikeExistsListener()
    asyncClient.exists(policy.orNull, el, key.inner)
    el.result
  }

  def header(key: AerospikeKey[_], policy: Option[Policy] = None): Future[AerospikeRecord] = {

    logger.debug(s"HEADER {$key}")

    val el = AerospikeReadListener()
    asyncClient.getHeader(policy.orNull, el, key.inner)
    el.result
  }

  def get(key: AerospikeKey[_], bins: Seq[String] = Seq.empty, policy: Option[Policy] = None): Future[AerospikeRecord] = {
    innerGet(key, bins, AerospikeReadListener(), policy)
  }

  def getOptional(key: AerospikeKey[_], bins: Seq[String] = Seq.empty, policy: Option[Policy] = None): Future[Option[AerospikeRecord]] = {
    innerGet(key, bins, AerospikeOptionalReadListener(), policy)
  }

  def getBin[V](key: AerospikeKey[_], bin: String, convert: AerospikeRecord => String => V, policy: Option[Policy] = None)
    (implicit ec: ExecutionContext): Future[Option[V]] = {

    logger.debug(s"GETBIN {$key} $bin")

    get(key, Seq(bin), policy).map { record =>
      Try(convert(record)(bin)).toOption
    }
  }

  def getMultiRecords[K](keys: Seq[AerospikeKey[K]], bins: Seq[String] = Seq.empty, policy: Option[BatchPolicy] = None)
    (implicit keyConverter: AerospikeKeyConverter[K]): Future[Map[AerospikeKey[K], AerospikeRecord]] = {

    logger.debug(s"GETMULTI {${keys.mkString(", ")}} ${bins.mkString(", ")}")

    val listener = AerospikeReadSequenceListener[K]()
    bins match {
      case Nil => asyncClient.get(policy.orNull, listener, keys.map(_.inner).toArray)
      case _ => asyncClient.get(policy.orNull, listener, keys.map(_.inner).toArray, bins: _*)
    }
    listener.result
  }

  private def innerGet[T](key: AerospikeKey[_], bins: Seq[String] = Seq.empty, listener: Listener[T] with RecordListener, policy: Option[Policy] = None): Future[T] = {
    logger.debug(s"GET {$key} ${bins.mkString(", ")}")

    bins match {
      case Nil => asyncClient.get(policy.orNull, listener, key.inner)
      case _ => asyncClient.get(policy.orNull, listener, key.inner, bins: _*)
    }
    listener.result
  }
}
