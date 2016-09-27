import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import com.aerospike.client.query.IndexType
import com.aerospike.client.{Value, Bin => AEBin}
import io.tabmo.aerospike.converter.key._
import io.tabmo.aerospike.data.{AerospikeKey, AerospikeKeyConverter, Bin}
import io.tabmo.aerospike.utils._
import io.tabmo.aerospike.{AerospikeClientTest, CustomSpec}

import shapeless.Generic

class ShapeQueryUsage extends CustomSpec with AerospikeClientTest {

  implicit val asClient = client
  val ns = "test"
  val set = "queryshape"

  def clean(key: AerospikeKey[_], keys: AerospikeKey[_]*) = (key +: keys).foreach(k => ready(client.delete(k)))

  def clean[K](data: Map[AerospikeKey[K], Seq[AEBin]]) = data.keys.foreach(k => ready(client.delete(k)))

  def insert[K](data: Map[AerospikeKey[K], Seq[AEBin]])(implicit keyConv: AerospikeKeyConverter[K]) = data.map { case (key, bins) => ready(client.put(key, bins)) }

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
    ready(client.registerUDF(this.getClass.getClassLoader, "persons.lua", "persons.lua"))
    Thread.sleep(4000)

    (data, Seq(index1, index2, index3))
  }

  def reset[K](data: Map[AerospikeKey[K], Seq[AEBin]], indices: Seq[String]) = {
    indices.foreach(i => ready(client.dropIndex(ns, set, i)))
    ready(client.removeUDF("persons.lua"))
    clean(data)
    clean(data)
  }


  case class PersonId(id: Long)
  case class Person(id: PersonId, name: String, age: Long)
  object Person {
    implicit val personGen = Generic[Person]
  }

  object QueryShapeSet extends AerospikeDataSet(ns, set) {

    import shapeless._
    import Person._
    import util._

    private implicit def idToPersonId(id: Long): PersonId = PersonId(id)
    private val id = longTo[PersonId]("id")
    private val name = string("name")
    private val age = long("age")

    private val setShape = shaped(id :: name :: age :: HNil).to[Person]
    val getPersonWithAgeEqualTo = setShape.prepareQueryEquals(age)
    val getPersonWithAgeBetween = setShape.prepareQueryRange(age)
    val getPersonByIndex = setShape.prepareQueryIndex

    private val setTupleShape = shaped(id :: name :: age :: HNil).tupled()
    val getTupleWithAgeBetween = setTupleShape.prepareQueryRange(age)
    val getTupleWithAgeEqualTo = setTupleShape.prepareQueryEquals(age)

    private val filterByAgeShape = shaped(name :: age :: HNil).tupled()
    val aggregateEqualById = filterByAgeShape.prepareQueryAggregateEquals("persons", "filterByAge", id)
    val aggregateRangeByAge = filterByAgeShape.prepareQueryAggregateRange("persons", "filterByAge", age)
  }

  "EQUALs query" should {

    "list records as Person by querying a LONG field" in {
      val (data, indices) = init()

      val result = QueryShapeSet.getPersonWithAgeEqualTo(18)

      whenReady(result) { r =>
        assert {
          r.size === 1
        }
        assert {
          r.head === Person(PersonId(1000), "thomas", 18)
        }
      }

      reset(data, indices)
    }

    "list records as Tuple by querying a LONG field" in {
      val (data, indices) = init()

      val result = QueryShapeSet.getTupleWithAgeEqualTo(18)

      whenReady(result) { r =>
        assert {
          r.size === 1
        }
        assert {
          val (id, name, age) = r.head
          id === PersonId(1000)
          name === "thomas"
          age === 18
        }
      }

      reset(data, indices)
    }
  }

  "RANGEs query" should {

    "list records as Person by querying a LONG field" in {
      val (data, indices) = init()

      val result = QueryShapeSet.getPersonWithAgeBetween(23, 24)

      whenReady(result) { r =>
        assert {
          r.size === 1
        }
        assert {
          r.head === Person(PersonId(2001), "pierre", 23)
        }
      }

      reset(data, indices)
    }

    "list records as Tuple by querying a LONG field" in {
      val (data, indices) = init()

      val result = QueryShapeSet.getTupleWithAgeBetween(23, 24)

      whenReady(result) { r =>
        assert {
          r.size === 1
        }
        assert {
          val (id, name, age) = r.head
          id === PersonId(2001)
          name === "pierre"
          age === 23
        }
      }

      reset(data, indices)
    }
  }

  "INDEXs query" should {

    "Get result as Person by index" in {
      val (data, indices) = init()

      val result = QueryShapeSet.getPersonByIndex(1)
      whenReady(result) { r =>
        assert {
          r.isDefined === true
        }
        assert {
          r.get === Person(PersonId(1000), "julien", 20)
        }
      }

      reset(data, indices)
    }
  }

  "Aggregate equal query" should {

    "Get result as tuple" in {
      val (data, indices) = init()

      import io.tabmo.aerospike.converter.value._
      val result = QueryShapeSet.aggregateEqualById(2001, 23)

      whenReady(result) { r =>
        assert {
          r.size === 1
        }
        assert {
          val (name, age) = r.head
          name === "pierre"
          age === 23
        }
      }

      reset(data, indices)
    }
  }

  "Aggregate ranges query" should {
    "Get result as tuple" in {
      val (data, indices) = init()

      import io.tabmo.aerospike.converter.value._
      val result = QueryShapeSet.aggregateRangeByAge(23, 24, 23)

      whenReady(result) { r =>
        assert {
          r.size === 1
        }
        assert {
          val (name, age) = r.head
          name === "pierre"
          age === 23
        }
      }

      reset(data, indices)
    }
  }
}
