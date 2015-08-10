package io.tabmo.aerospike.data

import scala.util.Try

import com.aerospike.client.Record

case class AerospikeRecord(bins: Map[String, AnyRef], generation: Int, expiration: Int) {

  private def get[T](column: String) = bins(column).asInstanceOf[T]

  def size = bins.size

  def exists(column: String) = bins.contains(column)

  def getLong(column: String) = get[Long](column)

  def getOptLong(column: String) = Try(getLong(column)).toOption

  def getString(column: String) = get[String](column)

  def getOptString(column: String) = Try(getString(column)).toOption

  def asByte(column: String) = getOptLong(column).map(_.toByte)

  def asInt(column: String) = getOptLong(column).map(_.toInt)

  def asShort(column: String) = getOptLong(column).map(_.toShort)

  def asFloat(column: String) = getOptString(column).map(_.toFloat)

  def asDouble(column: String) = getOptString(column).map(_.toDouble)

  def asBigDecimal(column: String) = getOptString(column).map(BigDecimal.apply).orElse(getOptLong(column).map(BigDecimal.apply))

  def asBigIng(column: String) = getOptLong(column).map(BigInt.apply).orElse(getOptString(column).map(BigInt.apply))
}

object AerospikeRecord {
  import scala.collection.JavaConverters._

  def apply(record: Record): AerospikeRecord = {
    val bins = Option(record.bins).map(_.asScala.toMap).getOrElse(Map.empty)
    AerospikeRecord(bins, record.generation, record.expiration)
  }
}

