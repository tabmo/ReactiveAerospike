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

import eu.unicredit.reactive_aerospike.data.AerospikeKey
import eu.unicredit.reactive_aerospike.data.AerospikeValue._
import eu.unicredit.reactive_aerospike.model.experimental._
import eu.unicredit.reactive_aerospike.data.AerospikeBinProto
import eu.unicredit.reactive_aerospike.data.AerospikeRecord

case class Mail(
  id: AerospikeKey[String],
  from: String,
  to: String,
  length: Long)

object MailDao extends Dao[String, Mail] {

  val namespace = "test"

  val setName = "mails"

  def getKeyDigest(obj: Mail): Array[Byte] =
    Dao.macroKeyDigest[Mail](obj)

  val objWrite: Seq[AerospikeBinProto[Mail, _]] =
    Dao.macroObjWrite[Mail]

  val objRead: (AerospikeKey[String], AerospikeRecord) => Mail =
    Dao.macroObjRead[Mail][String]

}