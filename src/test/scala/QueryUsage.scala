package io.tabmo.aerospike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.aerospike.client.query.IndexType
import com.aerospike.client.{Bin => AEBin}

import io.tabmo.aerospike.data.{AerospikeRecord, AerospikeKeyConverter, Bin, AerospikeKey}
import io.tabmo.aerospike.converter.key._
import io.tabmo.aerospike.converter.value.valueLongConverter

class QueryUsage extends CustomSpec with AerospikeClientTest {

  val ns = "test"
  val set = "unittest2"

  def clean(key: AerospikeKey[_], keys: AerospikeKey[_]*) = (key +: keys).foreach(k => ready(client.delete(k)))
  def clean[K](data: Map[AerospikeKey[K], Seq[AEBin]]) = data.keys.foreach(k => ready(client.delete(k)))
  def insert[K](data: Map[AerospikeKey[K], Seq[AEBin]])(implicit keyConv: AerospikeKeyConverter[K]) = data.map { case (key, bins) => ready(client.put(key, bins))}

  def init() = {
    val data = Map(
      AerospikeKey(ns, set, 1) -> Seq(Bin("id", 1000), Bin("name", "julien"), Bin("age", 20)),
      AerospikeKey(ns, set, 2) -> Seq(Bin("id", 1000), Bin("name", "thomas"), Bin("age", 18)),
      AerospikeKey(ns, set, 3) -> Seq(Bin("id", 2000), Bin("name", "thomas"), Bin("age", 22)),
      AerospikeKey(ns, set, 4) -> Seq(Bin("id", 2001), Bin("name", "pierre"), Bin("age", 23)),
      AerospikeKey(ns, set, 5) -> Seq(Bin("id", 2010), Bin("name", "henri"), Bin("age", 21))
    )

    insert(data)

    val index1 = result(client.createIndex(ns, set, "id", IndexType.NUMERIC))
    val index2 = result(client.createIndex(ns, set, "name", IndexType.STRING))
    val index3 = result(client.createIndex(ns, set, "age", IndexType.NUMERIC))

    (data, Seq(index1, index2, index3))
  }

  def reset[K](data: Map[AerospikeKey[K], Seq[AEBin]], indices: Seq[String]) = {
    indices.foreach(i => ready(client.dropIndex(ns, set, i)))
    clean(data)
  }

  "EQUALs query" should {

    "list records by querying a LONG field" in {
      val (data, indices) = init()

      val result: Future[Map[AerospikeKey[Long], AerospikeRecord]] = client.queryEqual[Long, Long](ns, set, Seq("id", "name"), "id", 1000)

      whenReady(result) { r =>
        assert { r.size === 2 }
        assert { r.head._2.getLong("id") === 1000 }
        assert {
          r.values.forall { r =>
            val name = r.getString("name")
            name == "julien" || name == "thomas"
          }
        }
      }

      reset(data, indices)
    }

    "list records by querying a STRING field" in {
      val (data, indices) = init()

      val result = client.queryEqual[Long, String](ns, set, Seq("id", "name"), "name", "thomas")

      whenReady(result) { r =>
        assert { r.size === 2 }
        assert { r.head._2.getString("name") === "thomas" }
        assert {
          r.values.forall { r =>
            val id = r.getLong("id")
            id == 1000 || id == 2000
          }
        }
      }

      reset(data, indices)
    }

    "allow to select all bins in a query" in {
      val (data, indices) = init()

      val result = client.queryEqual[Long, Long](ns, set, Seq.empty, "id", 1000)

      whenReady(result) { r =>
        assert { r.head._2.bins.keys.size === 3}
      }

      reset(data, indices)
    }

    "allow to filter bins in a query" in {
      val (data, indices) = init()

      val result = client.queryEqual[Long, Long](ns, set, Seq("id"), "id", 1000)

      whenReady(result) { r =>
        assert { r.head._2.bins.keys.size === 1}
      }

      reset(data, indices)
    }

  }

  "RANGE queries" should {

    "list records by querying on LONG field" in {
      val (data, indices) = init()

      val result = client.queryRange[Long](ns, set, Seq("id", "name"), "id", 2000, 2100)

      whenReady(result) { r =>
        val records = r.values.toList

        assert { records.size === 3 }
        assert { records.map(_.getLong("id")).sum === 6011 }
        assert { records.exists(_.getString("name") == "henri") }
      }

      reset(data, indices)
    }
  }

  "queryEqualAggregate operation" should {

    "return the map result as AerospikeRecord" in {

      val (data, indices) = init()

      ready(client.registerUDF(this.getClass.getClassLoader, "persons.lua", "persons.lua"))

      val result = client.queryEqualAggregate(ns, set,
        "name", "thomas",
        this.getClass.getClassLoader, "persons.lua",
        "persons", "filterByAge", Seq(19))

      whenReady(result) { r =>
        assert { r.size === 1 }
        assert { r.head.getLong("age") === 22 }
        assert { r.head.getOptString("age") === None }
      }

      ready(client.removeUDF("persons.lua"))
      reset(data, indices)
    }
  }

}
