package io.tabmo.aerospike

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Date

import com.aerospike.client.policy.WritePolicy
import org.scalatest.{BeforeAndAfterAll, Matchers}
import io.tabmo.aerospike.converter.key._
import io.tabmo.aerospike.data.{AerospikeKey, Bin}

class BasicUsage extends CustomSpec with BeforeAndAfterAll with AerospikeClientTest with Matchers {

  val ns = "test"
  val set = "unittest1"
  val neutral = Seq(Bin("v", 0))
  val complex = Seq(Bin("string", "hello"), Bin("long", 123L))

  def clean(key: AerospikeKey[_], keys: AerospikeKey[_]*) = (key +: keys).foreach(k => ready(client.delete(k)))

  override protected def afterAll() = {
    client.close()
  }

  "PUT operation" should {

    "allow to save a bin with String key and String value" in {
      val key = AerospikeKey(ns, set, "julien")
      val bin = Bin("name", "julien")
      val result = client.put(key, Seq(bin))

      whenReady(result) { r =>
        assert { r === key }
        assert { r.userKey === Some("julien") }
      }

      clean(key)
    }

    "forbid to save other types than Long or String" in {
      assertDoesNotCompile("""
         client.put(key, Bin("x", 1.0d))
      """)

      assertDoesNotCompile("""
         client.put(key, Bin("x", 1.0f))
      """)

      assertDoesNotCompile("""
         client.put(key, Bin("x", 'c'))
      """)

      val date = new Date
      assertDoesNotCompile("""
         client.put(key, Bin("x", date))
      """)
    }

    "allow to save a bin with Long key and Long value" in {
      val key = AerospikeKey(ns, set, 1)
      val bin = Bin("age", 26)
      val result = client.put(key, Seq(bin))

      whenReady(result) { r =>
        assert { r === key }
        assert { r.userKey === Some(1L) }
      }

      clean(key)
    }

    "allow to save mixed bin" in {
      val key = AerospikeKey(ns, set, "julien")
      val bins = Seq(Bin("name", "julien"), Bin("age", 26))
      val result = client.put(key, bins)

      whenReady(result) { r =>
        assert { r === key }
      }

      clean(key)
    }
  }

  "GET operation" should {

    val key = AerospikeKey(ns, set, "testget")
    val bins = Seq(Bin("foo", "bar"), Bin("max", Long.MaxValue), Bin("min", Long.MinValue))

    "Read all bins of a record" in {
      ready(client.put(key, bins))
      val result = client.get(key)

      whenReady(result) { r =>
        assert { r.getString("foo") === "bar" }
        assert { r.getLong("max") === Long.MaxValue }
        assert { r.getLong("min") === Long.MinValue }
      }

      clean(key)
    }

    "Read some bins of a record" in {
      ready(client.put(key, bins))
      val result = client.get(key, Seq("foo"))

      whenReady(result) { r =>
        assert { r.exists("foo") === true }
        assert { r.exists("max") === false }
      }

      clean(key)
    }

  }

  "ScanAll operation" should {

    val set = "scanall"
    def akey(key: String): AerospikeKey[String] = AerospikeKey(ns, set, key)

    "Read all bins of a record" in {
      ready(client.put(akey("pk#01"), Seq(Bin("data", "#01"))))
      ready(client.put(akey("pk#02"), Seq(Bin("data", "#02"))))
      ready(client.put(akey("pk#03"), Seq(Bin("data", "#03"))))

      val result = client.scanAll[String](ns, set)
      whenReady(result) { r =>
        assert { r.seq.size === 3 }
        r.values.map(_.getString("data")) should contain theSameElementsAs List("#01", "#02", "#03")
      }

      clean(akey("pk#01"), akey("pk#02"), akey("pk#03"))
    }
  }

  "APPEND operation" should {

    val key = AerospikeKey(ns, set, "testappend")

    "concatenate a string in a bin " in {
      ready(client.put(key, Seq(Bin("foo", "hello"))))
      ready(client.append(key, Seq(Bin("foo", "world"))))
      val result = client.get(key, Seq("foo"))

      whenReady(result) { r =>
        assert { r.getString("foo") === "helloworld" }
      }

      clean(key)
    }
  }

  "PREPEND operation" should {

    val key = AerospikeKey(ns, set, "testprepend")

    "prepend a string in a bin " in {
      ready(client.put(key, Seq(Bin("foo", "hello"))))
      ready(client.prepend(key, Seq(Bin("foo", "world"))))
      val result = client.get(key, Seq("foo"))

      whenReady(result) { r =>
        assert { r.getString("foo") === "worldhello" }
      }

      clean(key)
    }
  }

  "ADD operation" should {

    val key = AerospikeKey(ns, set, "testadd")
    val base = Seq(Bin("v1", 0), Bin("v2", 0), Bin("v3", Long.MaxValue), Bin("v4", Long.MinValue))
    val op = Seq(Bin("v1", -1000), Bin("v2", 1000), Bin("v3", -Long.MaxValue), Bin("v4", -Long.MinValue))

    "increment and decrement a bin" in {
      ready(client.put(key, base))
      ready(client.add(key, op))
      val result = client.get(key)

      whenReady(result) { r =>
        assert { r.getLong("v1") === -1000 }
        assert { r.getLong("v2") === 1000 }
        assert { r.getLong("v3") === 0 }
        assert { r.getLong("v4") === 0 }
      }

      clean(key)
    }
  }

  "DELETE operation" should {

    val key = AerospikeKey(ns, set, "testdelete")
    val unknownKey = AerospikeKey(ns, set, "testdelete2")
    val base = Seq(Bin("a", 0), Bin("b", 0))

    "return true when deleting an existing record" in {
      ready(client.put(key, base))
      val delete = client.delete(key)

      whenReady(delete) { existed =>
        assert { existed === true }
      }

      val exists = client.exists(key)

      whenReady(exists) { exists =>
        assert { exists === false }
      }

      clean(key)
    }

    "return false when deleting a non existing record" in {
      val delete = client.delete(unknownKey)

      whenReady(delete) { existed =>
        assert { existed === false }
      }
    }
  }

  "TOUCH operation" should {

    val key = AerospikeKey(ns, set, "testtouch")

    /*"create an empty record if the key don't exists" in {
      ready(client.delete(key))
      ready(client.touch(key))
      val result = client.get(key)

      result.onFailure { case ex => ex.printStackTrace() }

      whenReady(result) { r =>
        println(r.expiration)
        assert { r.size === 0 }
        assert { r.generation === 0 }

      }
    }*/

    "Update the generation of an existing record" in {
      ready(client.put(key, neutral))
      val r1 = result(client.get(key))
      ready(client.touch(key))
      val r2 = result(client.get(key))

      assert { r2.generation === r1.generation + 1 }

      clean(key)
    }

    "Reset the expiration of a record" in {
      val writePolicy10s = {
        val p = new WritePolicy(client.asyncClient.asyncWritePolicyDefault)
        p.expiration = 10
        p
      }

      val writePolicy100s = {
        val p = new WritePolicy(client.asyncClient.asyncWritePolicyDefault)
        p.expiration = 100
        p
      }

      ready(client.put(key, neutral, Some(writePolicy10s)))
      val r1 = result(client.get(key))
      ready(client.touch(key, Some(writePolicy100s)))
      val r2 = result(client.get(key))

      assert { r2.expiration > r1.expiration }

      clean(key)
    }
  }

  "EXISTS operation" should {


    "Return false if the record doesn't exist" in {
      val key = AerospikeKey(ns, set, "notexist")
      val result = client.exists(key)

      whenReady(result) { exists =>
        assert { exists === false }
      }
    }

    "Return true if the record exists" in {
      val key = AerospikeKey(ns, set, "testexists")
      ready(client.put(key, neutral))
      val result = client.exists(key)

      whenReady(result) { exists =>
        assert { exists === true }
      }

      clean(key)
    }
  }

  "HEADER operation" should {
    val key = AerospikeKey(ns, set, "testheader")

    "Return onlu the generation and the expiration data" in {
      ready(client.put(key, neutral))
      val result = client.header(key)

      whenReady(result) { r =>
        assert { r.expiration > 0 }
        assert { r.generation > 0 }
        assert { r.size === 0 }
      }
    }
  }

  "GETBIN operation" should {
    val key = AerospikeKey(ns, set, "testgetbin")

    "extract a single bin and cast it" in {
      ready(client.put(key, complex))

      val vString = result(client.getBin[String](key, "string", _.getString))
      val vLong = result(client.getBin[Long](key, "long", _.getLong))
      val invalidBin = result(client.getBin[String](key, "long", _.getString))

      assert { vString === Some("hello") }
      assert { vLong === Some(123L) }
      assert { invalidBin === None }

      clean(key)
    }
  }

  "getMultiRecords operation" should {
    val key1 = AerospikeKey(ns, set, "testgetmulti1")
    val key2 = AerospikeKey(ns, set, "testgetmulti2")

    "return a list of full records" in {
      ready(client.put(key1, complex))
      ready(client.put(key2, complex))
      val result = client.getMultiRecords(Seq(key1, key2))

      whenReady(result) { r =>
        assert { r.size === 2 }
        assert { r.forall(_._2.getLong("long") === 123L) }
        r.map(_._1.userKey) should contain allOf(key1.userKey, key2.userKey)
      }

      clean(key1, key2)
    }

  }

}
