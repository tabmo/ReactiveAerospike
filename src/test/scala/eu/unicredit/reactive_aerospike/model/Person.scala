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

package eu.unicredit.reactive_aerospike.model

import eu.unicredit.reactive_aerospike.model._
import eu.unicredit.reactive_aerospike.data.{ AerospikeKey, AerospikeRecord, AerospikeBinProto }
import eu.unicredit.reactive_aerospike.client.AerospikeClient
import eu.unicredit.reactive_aerospike.future.Future

case class Person(
  id: String,
  name: String,
  surname: String,
  age: Int)(implicit dao: OriginalKeyDao[String, Person])
    extends OriginalKeyModelObj[String](id, dao) with EqualPerson {
}

case class PersonDao() extends OriginalKeyDao[String, Person] {

  val namespace = "test"

  val setName = "people"

  import eu.unicredit.reactive_aerospike.data.AerospikeValue._
  val objWrite: Seq[AerospikeBinProto[Person, _]] =
    Seq(("name", (p: Person) => p.name),
      ("surname", (p: Person) => p.surname),
      ("age", (p: Person) => p.age))

  val objRead: (AerospikeKey[String], AerospikeRecord) => Person =
    (key: AerospikeKey[String], record: AerospikeRecord) =>
      Person(
        key.userKey.get,
        record.get("name"),
        record.get("surname"),
        record.get("age"))
}

case class GenericPerson(
  id: String,
  name: String,
  surname: String,
  age: Int) extends EqualGenericPerson {}

case class GenericPersonDao() extends Dao[String, GenericPerson] {

  val namespace = "test"

  val setName = "people"

  def getKeyDigest(obj: GenericPerson): Array[Byte] =
    AerospikeKey(namespace, setName, obj.id).digest

  val objWrite: Seq[AerospikeBinProto[GenericPerson, _]] =
    Seq(("name", (p: GenericPerson) => p.name),
      ("surname", (p: GenericPerson) => p.surname),
      ("age", (p: GenericPerson) => p.age))

  val objRead: (AerospikeKey[String], AerospikeRecord) => GenericPerson =
    (key: AerospikeKey[String], record: AerospikeRecord) =>
      GenericPerson(
        new String(key.digest),
        record.get("name"),
        record.get("surname"),
        record.get("age"))
}

trait EqualPerson {
  self: Person =>
  override def equals(p2: Any) = {
    p2 match {
      case per: Person =>
        (per.id == self.id &&
          per.name == self.name &&
          per.surname == self.surname &&
          per.age == self.age)
      case _ => false
    }
  }
}

trait EqualGenericPerson {
  self: GenericPerson =>
  override def equals(p2: Any) = {
    p2 match {
      case per: GenericPerson =>
        (
          per.name == self.name &&
          per.surname == self.surname &&
          per.age == self.age)
      case _ => false
    }
  }
}