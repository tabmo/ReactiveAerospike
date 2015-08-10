package io.tabmo.aerospike

import com.aerospike.client.Value

import io.tabmo.aerospike.data.AerospikeKeyConverter

package object converter {

  implicit val keyLongConverter = new AerospikeKeyConverter[Long] {
    override def convert(userKey: Value) = userKey.toLong
  }

  implicit val keyStringConverter = new AerospikeKeyConverter[String] {
    override def convert(userKey: Value) = userKey.toString
  }

}
