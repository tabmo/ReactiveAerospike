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

class MacroUsage extends FlatSpec {

  /*
   * NOTE: 
   * this implementation is in development!
   */

  "The DaoMacro " should "generate code to manage basic case classes " in {

    implicit val client = AerospikeClient("localhost", 3000)

    val key: AerospikeKey[String] = AerospikeKey[String](MailDao.namespace, MailDao.setName, "prova")

    val mail = Mail(
      key,
      "Andrea",
      "Marco",
      10)

    try
      Await.result(MailDao.delete(key), 500.millis)
    catch {
      case _: Throwable =>
    }

    Await.result(MailDao.create(mail), 500.millis)

    val ret = Await.result(MailDao.read(key), 100.millis)

    assert { mail.from == ret.from }
    assert { mail.to == ret.to }
    assert { mail.length == ret.length }

  }

}