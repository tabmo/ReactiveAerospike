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
import eu.unicredit.reactive_aerospike.data.{ AerospikeKey, AerospikeBinProto, AerospikeRecord }

import play.api.libs.json._ // JSON library
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._ //
import scala.reflect.ClassTag

object JsonMailDao extends Dao[String, Mail] {

  val namespace = "test"

  val setName = "mails"

  def getKeyDigest(obj: Mail): Array[Byte] =
    Dao.macroKeyDigest[Mail](obj)

  val objWrite: Seq[AerospikeBinProto[Mail, _]] =
    Dao.macroObjWrite[Mail]

  val objRead: (AerospikeKey[String], AerospikeRecord) => Mail =
    Dao.macroObjRead[Mail][String]

  implicit def aerospikeKeyWrites[T: ClassTag]: Writes[AerospikeKey[T]] = new Writes[AerospikeKey[T]] {
    def writes(ak: AerospikeKey[T]) =
      JsArray(ak.digest.map(b => Json.toJson(b)).toList)
  }

  implicit def AerospikeKeyReads[T: ClassTag](implicit keyConverter: AerospikeValueConverter[T]): Reads[AerospikeKey[T]] = new Reads[AerospikeKey[T]] {
    def reads(json: JsValue) =
      try {
        new JsSuccess(
          AerospikeKey(namespace, setName, json.validate[List[Byte]].map(_.toArray).get)(keyConverter))
      } catch {
        case err: Throwable => new JsError(Seq()) //To be improved
      }
  }

  val mailJsonReads = Json.reads[Mail]

  val mailJsonWrites = Json.writes[Mail]

}