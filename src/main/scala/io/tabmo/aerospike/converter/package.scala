package io.tabmo.aerospike

import com.aerospike.client.Value
import com.aerospike.client.Value.{StringValue, LongValue}

import io.tabmo.aerospike.data.AerospikeKeyConverter

package object converter {

  object key {

    // Key converters
    implicit val keyLongConverter: AerospikeKeyConverter[Long] = new AerospikeKeyConverter[Long] {
      override def convert(userKey: Value) = userKey.toLong
    }

    implicit val keyStringConverter: AerospikeKeyConverter[String] = new AerospikeKeyConverter[String] {
      override def convert(userKey: Value) = userKey.toString
    }
  }

  object value {

    // Value converters
    implicit def valueLongConverter(value: Long): Value = new LongValue(value)

    implicit def valueStringConverter(value: String): Value = new StringValue(value)

    implicit def valueIntConverter(value: Int): Value = new LongValue(value.toLong)
  }
}
