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

package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.Key
import scala.language.existentials
import AerospikeValue.AerospikeValueConverter

case class AerospikeKey[T <: Any](
    namespace: String,
    setName: String,
    userKey: AerospikeValue[T],
    converter: AerospikeValueConverter[T]) {
  
  val inner = new Key(namespace, setName, userKey)

}

object AerospikeKey {

  def apply[T <: Any](namespace: String,
    setName: String,
    userKey: T)
  	(implicit converter: AerospikeValueConverter[T]): AerospikeKey[T] =
    AerospikeKey(
      namespace,
      setName,
      converter.toAsV(userKey),
      converter
    )

  def apply[T <: Any](key: Key)
  	(implicit converter: AerospikeValueConverter[T]): AerospikeKey[T] =
    AerospikeKey(
      key.namespace,
      key.setName,
      converter.fromValue(key.userKey),
      converter
      )

}