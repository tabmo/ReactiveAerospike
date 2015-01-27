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
import eu.unicredit.reactive_aerospike.data.{ AerospikeKey, AerospikeRecord, AerospikeBinProto }
import eu.unicredit.reactive_aerospike.client.AerospikeClient
import eu.unicredit.reactive_aerospike.data.AerospikeValue
import eu.unicredit.reactive_aerospike.data.AerospikeValue._

import eu.unicredit.reactive_aerospike.tests.crypt.AerospikeCryptValue._

case class Poem(
  key: AerospikeKey[String],
  author: String,
  title: String,
  text: String)(implicit dao: Dao[String, Poem])
    extends ModelObj[String](key.inner.digest, dao) {

  def this(keyS: String,
    author: String,
    title: String,
    text: String)(implicit dao: Dao[String, Poem]) =
    this(AerospikeKey[String](dao.namespace, dao.setName, keyS),
      author,
      title,
      text)

}

case class PoemDao(passwordString: Option[String], client: AerospikeClient = new AerospikeClient("localhost", 3000))
    extends DigestDao[String, Poem](client) {
  val namespace = "test"

  val setName = "poems"

  val password = passwordString.map(AESKey(_))

  def text(content: String): AerospikeValue[String] =
    if (password.isDefined)
      AerospikeAESString(content, password.get)
    else
      AerospikeString(content)

  val textBinProto: AerospikeBinProto[Poem, String] =
    if (password.isDefined)
      AerospikeBinProto[Poem, String]("text", (p: Poem) => AerospikeAESString(p.text, password.get).base, AerospikeAESStringConverter(password.get))
    else
      AerospikeBinProto[Poem, String]("text", (p: Poem) => p.text, AerospikeStringConverter)

  val objWrite: Seq[AerospikeBinProto[Poem, _]] =
    Seq(("author", (p: Poem) => p.author),
      ("title", (p: Poem) => p.title),
      textBinProto)

  val objRead: (AerospikeKey[String], AerospikeRecord) => Poem =
    (key: AerospikeKey[String], record: AerospikeRecord) =>
      Poem(
        key,
        record.get("author"),
        record.get("title"),
        record.get("text"))

}
