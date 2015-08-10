package io.tabmo.aerospike.data

import scala.util.Try

import com.aerospike.client.command.Buffer
import com.aerospike.client.{Value, Key}

case class AerospikeKey[T](inner: Key)(implicit keyConverter: AerospikeKeyConverter[T]) {
  val namespace = inner.namespace
  val setName = Option(inner.setName)
  val userKey = {
    Try {
      inner.userKey.validateKeyType()
      keyConverter.convert(inner.userKey)
    }.toOption
  }

  override def toString = {
    val stringKey = userKey.map(_.toString).getOrElse(Buffer.bytesToHexString(inner.digest))
    namespace + ":" + setName.getOrElse("/") + ":" + stringKey
  }
}

object AerospikeKey {

  import io.tabmo.aerospike.converter._

  def apply(namespace: String, setName: String, value: Long): AerospikeKey[Long] = {
    AerospikeKey[Long](new Key(namespace, setName, value))
  }

  def apply(namespace: String, setName: String, value: String): AerospikeKey[String] = {
    AerospikeKey[String](new Key(namespace, setName, value))
  }

}

trait AerospikeKeyConverter[T] {
  def convert(userKey: Value): T
}
