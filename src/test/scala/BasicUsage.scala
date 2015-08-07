/*
* Copyright 2014 UniCredit S.p.A.
* Copyright 2014 Tabmo.io
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package io.tabmo.aerospike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

import com.aerospike.client.query.IndexType

import org.scalatest._

import io.tabmo.aerospike.client._
import io.tabmo.aerospike.data._
import io.tabmo.aerospike.data.AerospikeValue._

class BasicUsage extends FlatSpec {

  val client = ReactiveAerospikeClient("aerospikeovh1", 3000)

  val key = AerospikeKey("test", "demokey", "123")

  val x = 1.9
  val y = 2014L
  val z = "tre"

  "An Aerospike client" should "save sequences of bins under a given key" in {

    val bin1 = AerospikeBin("x", x)
    val bin2 = AerospikeBin("y", y)
    val bin3 = AerospikeBin("z", z)

    val aKey: Future[AerospikeKey[String]] =
      client.put(key, Seq(bin1, bin2, bin3))

    val result = Await.result(aKey, 5.seconds)

    assert { key == result }
  }

  it should "retrieve the record" in {
    //a record reader is required for reading
    val recordReader = new AerospikeRecordReader(
      Map("x" -> AerospikeDoubleConverter,
        "y" -> AerospikeLongConverter,
        "z" -> AerospikeStringConverter))

    //when reading/getting you always get back a couple (key, record)
    val (theKey, theRecord) =
      Await.result(
        client.get(key, recordReader),
        5.seconds)

    assert { key == theKey }
    assert { x == theRecord.getAs[Double]("x") }
    assert { y == theRecord.getAs[Long]("y") }
    assert { z == theRecord.getAs[String]("z") }
  }

  it should "save and retrieve complex data" in {
    import scala.language.existentials

    val aListOfInt = AerospikeList(1, 2, 3)
    val anotherListOfInt = AerospikeList(4, 5, 6)

    val myListOfLists = AerospikeList(aListOfInt, anotherListOfInt)

    val myMap = AerospikeMap("one" -> 1, "" +
      "two" -> 2)

    val complexPutOperation =
      client.put(key, Seq(
        AerospikeBin("myListOfLists", myListOfLists),
        AerospikeBin("myMap", myMap)))

    val aKey = Await.result(complexPutOperation, 5.seconds)

    assert { key == aKey }

    //expliciting the implicits just to show them
    implicit val listConverter = AerospikeListConverter()(AerospikeListConverter()(AerospikeIntConverter))
    implicit val mapConverter = AerospikeMapConverter()(AerospikeStringConverter, AerospikeIntConverter)

    val recordReader = new AerospikeRecordReader(
      Map("myListOfLists" -> listConverter, "myMap" -> mapConverter))

    val (theKey, theRecord) = Await.result(
      client.get(aKey, recordReader), //get
      5.seconds)

    assert { key == theKey }
    //Syntax have to be improved here... but engine works...
    assert { 4 == theRecord.get[List[AerospikeList[Int]]]("myListOfLists").base(1).base(0).base }
    assert { 2 == theRecord.get[Map[AerospikeString, AerospikeInt]]("myMap").base.map(x => x._1.base -> x._2.base).get("two").get }
  }

  /*it should "create and drop index" in {
    val result = Await.result(for {
      indexName <- client.createIndex("test", "persons", "name", IndexType.STRING)
      r1 <-  client.put(AerospikeKey("test", "persons", "julien"), Seq(AerospikeBin("name", "julien")))
      bin <- client.queryEqual(r1, new AerospikeRecordReader(Map("name" -> AerospikeStringConverter)), AerospikeBin("name", "julien"))
      _ <- client.dropIndex("test", "persons", indexName)
    } yield bin.head._2.get[String]("name").base, 10.seconds)

    assert { result == "julien" }
  }*/

  it should "call aggregate query" in {

    val result = Await.result(for {
      indexName <- client.createIndex("test", "persons", "name", IndexType.STRING)
      r1 <- client.put(AerospikeKey("test", "persons", "julien"), Seq(AerospikeBin("name", "julien"), AerospikeBin("age", 26)))
      r2 <- client.put(AerospikeKey("test", "persons", "julien2"), Seq(AerospikeBin("name", "julien"), AerospikeBin("age", 14)))
      r3 <- client.put(AerospikeKey("test", "persons", "pierre"), Seq(AerospikeBin("name", "pierre"), AerospikeBin("age", 20)))
      _ <- client.registerUDF(this.getClass.getClassLoader, "persons.lua", "persons.lua")
      r <- client.queryEqualStringAggregateMap(
        "test", "persons", "name", "julien",
        this.getClass.getClassLoader, "persons.lua",
        "persons", "filterByAge", 18
      )
      _ <- client.dropIndex("test", "persons", indexName)
      _ <- client.delete(r1)
      _ <- client.delete(r2)
      _ <- client.delete(r3)
      _ <- client.removeUDF("persons.lua")
    } yield r, 10.seconds)

    assert { result.size == 1 }
    assert { result.head("name") == "julien" }
    assert { result.head("age") == 26 }
  }

  it should "accept long values" in {
    val result = Await.result(for {
      key <- client.put(AerospikeKey("test", "io/tabmo/aerospike/data", "long"), Seq(AerospikeBin("value", Long.MaxValue)))
      bin <- client.get(key, new AerospikeRecordReader(Map("value" -> AerospikeLongConverter)))
      _ <- client.delete(key)
    } yield bin._2.getAs[Long]("value"), 10.seconds)

    assert { result == Long.MaxValue }
  }

  it should "allow to use low level methods" in {
    val result = Await.result(for {
      indexName <- client.createIndex("test", "persons", "age", IndexType.NUMERIC)
      r1 <- client.put(AerospikeKey("test", "persons", "julien"), Seq(AerospikeBin("name", "julien"), AerospikeBin("age", 26L)))
      r2 <- client.put(AerospikeKey("test", "persons", "julien2"), Seq(AerospikeBin("name", "julien"), AerospikeBin("age", 14L)))
      r3 <- client.put(AerospikeKey("test", "persons", "pierre"), Seq(AerospikeBin("name", "pierre"), AerospikeBin("age", 26L)))
      r <- client.rawQueryEqualLong("test", "persons", Seq("name", "age"), "age", 26)
      _ <- client.dropIndex("test", "persons", indexName)
      _ <- client.delete(r1)
      _ <- client.delete(r2)
      _ <- client.delete(r3)
    } yield r, 10.seconds)

    assert { result.size == 2 }
    assert { result.head._2.getLong("age") == 26 }
    assert { result.head._2.getString("name") == "julien" }
    assert { result.last._2.getString("name") == "pierre"}
  }

}
