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

class DaoUsage extends FlatSpec {

  "An Aerospike Client" should "save and retrieve a person using Scala Futures " in {
    import scala.concurrent.Await
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val client = new AerospikeClient("localhost", 3000)(ScalaFactory)

    implicit val personDao = PersonDao()

    val bob = Person("rOBerT", "Bob", "Wood", 23)
    val tim = Person("TImoTHy", "Tim", "Forest", 32)

    //cleanup
    try {
      Await.result(personDao.delete("rOBerT"), 100 millis)
      Await.result(personDao.delete("TImoTHy"), 100 millis)
    } catch {
      case _: Throwable =>
    }

    //creating
    Await.result(personDao.create(bob), 100 millis)
    Await.result(personDao.create(tim), 100 millis)

    // val res1 = Await.result(
    //   personDao.read("rOBerT"),
    //   100 millis)

    // val res2 = Await.result(
    //   personDao.read("TImoTHy"),
    //   100 millis)

    Await.result(personDao.read("TImoTHy"), 100 millis) == tim

    Await.result(personDao.read("rOBerT"), 100 millis) == bob

  }

  it should "save and retrieve a person with Twitter Futures" in {
    import com.twitter.conversions.time._
    import com.twitter.util._

    implicit val client = new AerospikeClient("localhost", 3000)(TwitterFactory)

    implicit val personDao = PersonDao()

    val john = Person("joHn", "John", "Doe", 23)
    val carl = Person("cARL", "Carl", "Green", 32)

    //cleanup
    try {
      Await.result(personDao.delete("joHn"), 500.millis)
      Await.result(personDao.delete("cARL"), 500.millis)
    } catch {
      case _: Throwable =>
    }

    Await.result(personDao.create(john), 500.millis)
    Await.result(personDao.create(carl), 100.millis)

    val res1 = Await.result(personDao.read("joHn"), 100.millis)
    val res2 = Await.result(personDao.read("cARL"), 100.millis)

    assert { john == res1 }
    assert { carl == res2 }
  }

  it should "save and retrieve generic case classes" in {
    import scala.concurrent.Await
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global

    import eu.unicredit.reactive_aerospike.data.AerospikeKey

    implicit val client = new AerospikeClient("localhost", 3000)(ScalaFactory)

    implicit val personDao = GenericPersonDao()

    val john = GenericPerson("joHn", "Tizio", "tizio", 23)
    val carl = GenericPerson("cARL", "Caio", "caio", 32)

    def gejoHn(ap: GenericPerson): AerospikeKey[String] =
      AerospikeKey(personDao.namespace, personDao.setName, ap.id)

    try {
      Await.result(personDao.delete(gejoHn(john)), 100 millis)
      Await.result(personDao.delete(gejoHn(carl)), 100 millis)
    } catch {
      case _: Throwable =>
    }

    Await.result(personDao.create(john), 100 millis)
    Await.result(personDao.create(carl), 100 millis)

    val retP1 = Await.result(personDao.read(gejoHn(john)), 100 millis)
    val retP2 = Await.result(personDao.read(gejoHn(carl)), 100 millis)

    assert { john == retP1 }
    assert { carl == retP2 }
  }

}