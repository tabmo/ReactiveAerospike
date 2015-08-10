package io.tabmo.aerospike

import com.aerospike.client.Value
import com.aerospike.client.Value.{StringValue, LongValue}

import io.tabmo.aerospike.data.AerospikeKeyConverter

package object converter {

  // Key converters
  implicit val keyLongConverter = new AerospikeKeyConverter[Long] {
    override def convert(userKey: Value) = userKey.toLong
  }

  implicit val keyStringConverter = new AerospikeKeyConverter[String] {
    override def convert(userKey: Value) = userKey.toString
  }

  // Value converters
  implicit def valueLongConverter(value: Long): Value = new LongValue(value)
  implicit def valueStringConverter(value: String): Value = new StringValue(value)
  implicit def valueIntConverter(value: Int): Value = new LongValue(value.toLong)
}
