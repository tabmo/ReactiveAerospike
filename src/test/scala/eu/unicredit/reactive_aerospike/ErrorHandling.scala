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

package eu.unicredit.reactive_aerospike

import org.scalatest._
import java.util.UUID
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.model._
import eu.unicredit.reactive_aerospike.client.AerospikeClient
import eu.unicredit.reactive_aerospike.future.ScalaFactory
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class ErrorHandling extends FlatSpec {

  case class Mess(
    id: String,
    name: String,
    surname: String,
    age: Int)(implicit dao: OriginalKeyDao[String, Mess])
      extends OriginalKeyModelObj[String](id, dao) {
  }

  case class MessDao(wrongName: Boolean, wrongType: Boolean) extends OriginalKeyDao[String, Mess] {

    val namespace = "test"

    val setName = "mess"

    import eu.unicredit.reactive_aerospike.data.AerospikeValue._
    val objWrite: Seq[AerospikeBinProto[Mess, _]] =
      Seq(("name", (p: Mess) => p.name),
        ("surname", (p: Mess) => p.surname),
        ("age", (p: Mess) => p.age))

    val nameReader: (AerospikeRecord => String) = (record: AerospikeRecord) =>
      if (wrongName)
        record.get("n4m3")
      else
        record.get[String]("name")

    val ageReader: (AerospikeRecord => Int) = (record: AerospikeRecord) =>
      if (wrongType)
        record.get[String]("age").asInstanceOf[String].toInt
      else
        record.get("age")

    val objRead: (AerospikeKey[String], AerospikeRecord) => Mess =
      (key: AerospikeKey[String], record: AerospikeRecord) =>
        Mess(key.userKey.get,
          nameReader(record),
          record.get("surname"),
          ageReader(record))
  }

  "The Dao " should "retrieve menaningful parsing error messages with wrong bin names " in {

    implicit val client = AerospikeClient("localhost", 3000)

    implicit val messDao = MessDao(true, false)

    val bob = Mess("rOBerT", "Bob", "Wood", 23)

    try
      Await.result(messDao.delete("rOBerT"), 500.millis)
    catch {
      case _: Throwable =>
    }

    Await.result(messDao.create(bob), 500.millis)

    try
      Await.result(messDao.read("rOBerT"), 100.millis)
    catch {
      case err: Throwable =>
        val stackTrace = err.getCause().getMessage()
        assert { stackTrace === "Bin name n4m3 not found" }
    }

  }

  it should "retrieve menaningful parsing error messages with wrong bin types casts" in {

    implicit val client = AerospikeClient("localhost", 3000)

    implicit val messDao = MessDao(false, true)

    val bob = Mess("rOBerT", "Bob", "Wood", 23)

    try
      Await.result(messDao.delete("rOBerT"), 500.millis)
    catch {
      case _: Throwable =>
    }

    Await.result(messDao.create(bob), 500.millis)

    try
      Await.result(messDao.read("rOBerT"), 100.millis)
    catch {
      case err: Throwable =>
        val stackTrace = err.getCause().getMessage()
        assert { stackTrace === "eu.unicredit.reactive_aerospike.data.AerospikeValue$AerospikeInt cannot be cast to java.lang.String" }
    }

  }
}
