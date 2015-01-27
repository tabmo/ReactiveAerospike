/* Copyright 2014 UniCredit S.p.A.
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

package eu.unicredit.reactive_aerospike.tests

import org.scalatest._

import eu.unicredit.reactive_aerospike.client._
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.data.AerospikeValue._
import eu.unicredit.reactive_aerospike.future.ScalaFactory.Helpers._

import scala.util.{ Success, Failure }
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class BasicUsage extends FlatSpec {

  val client = new AerospikeClient("localhost", 3000)

  val key = AerospikeKey("test", "demokey", "123")

  val x = 1.9
  val y = 2014L
  val z = "tre"

  "An Aerospike client" should "save sequences of bins under a given key" in {
    import eu.unicredit.reactive_aerospike.future.{ Future => ReactiveAerospikeFuture }

    val bin1 = AerospikeBin("x", x)
    val bin2 = AerospikeBin("y", y)
    val bin3 = AerospikeBin("z", z)

    val aKey: ReactiveAerospikeFuture[AerospikeKey[String]] =
      client.put(key, Seq(bin1, bin2, bin3))

    val result = Await.result(aKey, 100 millis)

    assert { key == result }
  }

  it should "retrieve the record" in {
    import eu.unicredit.reactive_aerospike.future.{ Future => ReactiveAerospikeFuture }
    import scala.language.existentials

    //a record reader is required for reading
    val recordReader = AerospikeRecordReader(
      Map(("x" -> AerospikeDoubleConverter),
        ("y" -> AerospikeLongConverter),
        ("z" -> AerospikeStringConverter)))

    //when reading/getting you always get back a couple (key, record)
    val (theKey, theRecord) =
      Await.result(
        client.get(key, recordReader),
        100 millis)

    assert { key == theKey }
    assert { x == theRecord.get[Double]("x").base }
    assert { y == theRecord.get[Long]("y").base }
    assert { z == theRecord.get[String]("z").base }
  }

  it should "save and retrieve complex data" in {
    import scala.language.existentials

    val aListOfInt = AerospikeList(1, 2, 3)
    val anotherListOfInt = AerospikeList(4, 5, 6)

    val myListOfLists = AerospikeList(aListOfInt, anotherListOfInt)

    val myMap = AerospikeMap(("one" -> 1), ("two" -> 2))

    val complexPutOperation =
      client.put(key, Seq(
        AerospikeBin("myListOfLists", myListOfLists),
        AerospikeBin("myMap", myMap)))

    val aKey = Await.result(complexPutOperation, 100 millis)

    assert { key == aKey }

    //expliciting the implicits just to show them
    implicit val listConverter = AerospikeValue.listConverter[Int]
    implicit val mapConverter = new AerospikeValue.AerospikeTupleConverter[String, Int]

    val recordReader = AerospikeRecordReader(
      Map(("myListOfLists" -> listConverter), ("myMap" -> mapConverter)))

    val (theKey, theRecord) = Await.result(
      client.get(aKey, recordReader), //get
      100 millis)

    assert { key == theKey }
    //Syntax have to be improved here... but engine works...
    assert { 4 == theRecord.get[List[AerospikeList[Int]]]("aList").base(1).base(0).base }
    assert { 2 == theRecord.get[Map[AerospikeString, AerospikeInt]]("aMap").base.map(x => x._1.base -> x._2.base).get("due").get }
  }

}