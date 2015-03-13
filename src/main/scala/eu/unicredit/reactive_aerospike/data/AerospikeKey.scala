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
import AerospikeValue.AerospikeValueConverter
import AerospikeValue.AerospikeNull

case class AerospikeKey[T <: Any](
    namespace: String,
    digest: Array[Byte],
    originalSetName: Option[String] = None,
    originalUserKey: Option[AerospikeValue[T]] = None)(implicit _converter: AerospikeValueConverter[T]) {

  val setName = originalSetName
  val userKey = originalUserKey

  val converter = _converter

  val inner = new Key(namespace, digest, setName.getOrElse(null),
    {
      try {
        converter.toAsV(userKey.get)
      } catch {
        case _: Throwable => AerospikeNull()
      }
    }
  )

  override def equals(k: Any): Boolean = k match {
    case key: AerospikeKey[T] => this.inner == key.inner
    case _ => false
  }

}

object AerospikeKey {

  def computeDigest[T <: Any](setName: String, key: AerospikeValue[T]): Array[Byte] =
    Key.computeDigest(setName, key.inner)

  def apply[T <: Any](namespace: String,
    setName: String,
    userKey: T)(implicit converter: AerospikeValueConverter[T]): AerospikeKey[T] =
    AerospikeKey(
      namespace,
      computeDigest(setName,
        converter.toAsV(userKey)),
      Some(setName), Some(converter.toAsV(userKey)))(converter)

  def apply[T <: Any](key: Key)(implicit converter: AerospikeValueConverter[T]): AerospikeKey[T] =
    AerospikeKey(
      key.namespace,
      key.digest,
      {
        if (key.setName != null) {
          Some(key.setName)
        } else {
          None
        }
      },
      {
        try {
          key.userKey.validateKeyType()
          Some(converter.fromValue(key.userKey))
        } catch {
          case _: Throwable => None
        }
      })(converter)

  def apply[T <: Any](namespace: String,
    digest: Array[Byte],
    converter: AerospikeValueConverter[T]): AerospikeKey[T] =
    AerospikeKey(
      namespace,
      digest,
      None, None
    )(converter)

  def apply[T <: Any](namespace: String,
    setName: String,
    digest: Array[Byte])(
      implicit converter: AerospikeValueConverter[T]): AerospikeKey[T] =
    AerospikeKey(
      namespace,
      digest,
      Some(setName), None
    )(converter)

  def apply[T <: Any](
    namespace: String,
    setName: String,
    userKey: AerospikeValue[T],
    converter: AerospikeValueConverter[T]): AerospikeKey[T] =
    AerospikeKey(
      namespace,
      computeDigest(setName,
        userKey),
      Some(setName), Some(userKey))(converter)
}
