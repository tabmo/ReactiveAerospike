package io.tabmo.aerospike.data

import com.aerospike.client.{ Bin => OriginalBin }

import io.tabmo.aerospike.TSafe.VRestriction

object Bin {
  def apply(name: String, value: Long): OriginalBin = new OriginalBin(name, value)

  def apply(name: String, value: String): OriginalBin = new OriginalBin(name, value)

  def apply[V: VRestriction](name: String, value: V): OriginalBin = {
    value match {
      case l: Long => apply(name, l)
      case i: Int => apply(name, i.toLong)
      case s: String => apply(name, s)
    }
  }
}
