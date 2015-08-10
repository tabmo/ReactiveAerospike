package io.tabmo.aerospike

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.BeforeAndAfterAll

import io.tabmo.aerospike.client._
import io.tabmo.aerospike.converter._
import io.tabmo.aerospike.data.AerospikeOperations._
import io.tabmo.aerospike.data.{AerospikeKey, Bin}

class OperateUsage extends CustomSpec with BeforeAndAfterAll {

  val client = ReactiveAerospikeClient.connect("aerospiketestserver", 3000)

  val ns = "test"
  val set = "unittest3"
  val key = AerospikeKey(ns, set, 0)
  val neutral = Seq(Bin("string", "hello"), Bin("v", 0))

  def init() = ready(client.put(key, neutral))
  def reset() = ready(client.delete(key))

  override protected def afterAll() = {
    client.close()
  }

  "OPERATE operation" should {

    "execute multiple operations and returning the record with the final `getAll`" in {
      init()

      val result = client.operate(key,
        put("foo", "bar"),
        put("long", 1000),
        add("v", 1),
        append("string", "world"),
        getAll)()

      result.onFailure { case ex => ex.printStackTrace() }

      whenReady(result) { r =>
        assert { r.isDefined }
        assert { r.get.getLong("v") === 1 }
        assert { r.get.getString("foo") === "bar" }
        assert { r.get.getLong("long") === 1000 }
        assert { r.get.getString("string") === "helloworld" }
      }

      reset()
    }

    "execute multiple operations and returning a bin with the final `get`" in {
      init()

      val result = client.operate(key, put("foo", "bar"), get("foo"))()

      whenReady(result) { r =>
        assert { r.isDefined === true }
        assert { r.get.getString("foo") === "bar" }
        assert { r.get.getOptLong("v") === None }
      }

      reset()
    }

    "execute multiple operations and returning None in all other cases" in {
      init()

      val result = client.operate(key, put("foo", "bar"))()

      whenReady(result) { r =>
        assert { r.isEmpty }
      }

      reset()
    }

    "doesn't compile if a wrong type is given" in {
      assertDoesNotCompile("""
        client.operate(key, put("x", 1f))
      """)

      assertDoesNotCompile("""
        client.operate(key, put("x", 1d))
      """)

      assertDoesNotCompile("""
        client.operate(key, put("x", 'c'))
      """)
    }

  }

}
