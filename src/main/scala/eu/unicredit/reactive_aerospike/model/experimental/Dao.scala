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

package eu.unicredit.reactive_aerospike.model.experimental

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import eu.unicredit.reactive_aerospike.data.{ AerospikeKey, AerospikeBinProto, AerospikeRecord }

trait DigestGenerator[T] {
  def get: (T) => Array[Byte]
}

trait BinSeqGenerator[T] {
  def get: Seq[AerospikeBinProto[T, _]]
}

trait ObjReadGenerator[T] {
  def apply[K]: (AerospikeKey[K], AerospikeRecord) => T
}

object Dao {

  def macroKeyDigest[T]: (T) => Array[Byte] = macro DaoMacroImpl.materializeDigestGennerator[T]

  def macroObjWrite[T]: Seq[AerospikeBinProto[T, _]] = macro DaoMacroImpl.materializeBinSeqGennerator[T]

  def macroObjRead[T]: ObjReadGenerator[T] = macro DaoMacroImpl.materializeObjReadGennerator[T]

}