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

package eu.unicredit.reactive_aerospike.tests.model

import eu.unicredit.reactive_aerospike.model._
import eu.unicredit.reactive_aerospike.data.{AerospikeKey, AerospikeRecord, AerospikeBinProto}
import eu.unicredit.reactive_aerospike.client.AerospikeClient

case class Person(id: String,
  name: String,
  surname: String,
  age: Int)
  (implicit dao: OriginalKeyDao[String, Person])
  extends OriginalKeyModelObj[String](id, dao) with EqualPerson {

}

case class PersonDao(client: AerospikeClient) extends OriginalKeyDao[String, Person](client) {

  val namespace = "test"

  val setName = "people"

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