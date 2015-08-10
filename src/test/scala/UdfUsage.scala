package io.tabmo.aerospike

import org.scalatest.BeforeAndAfterAll

import io.tabmo.aerospike.client._
import io.tabmo.aerospike.data.{AerospikeKey, Bin}

class UdfUsage extends CustomSpec with BeforeAndAfterAll {

  val client = ReactiveAerospikeClient.connect("aerospiketestserver", 3000)

  val ns = "test"
  val set = "unittest4"
  val neutral = Seq(Bin("v", 0))
  val complex = Seq(Bin("string", "hello"), Bin("long", 123L))

  def clean(key: AerospikeKey[_], keys: AerospikeKey[_]*) = (key +: keys).foreach(k => ready(client.delete(k)))

  override protected def afterAll() = {
    client.close()
  }

}
