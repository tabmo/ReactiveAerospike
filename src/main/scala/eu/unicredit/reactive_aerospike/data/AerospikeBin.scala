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

import com.aerospike.client.{ Bin, Value }
import AerospikeValue.{ AerospikeValueConverter, AerospikeList, AerospikeMap }

case class AerospikeBin[T <: Any](name: String, value: AerospikeValue[T], converter: AerospikeValueConverter[T]) {
  val inner = new Bin(name, value.inner)
  def toRecordValue = name -> value.inner.getObject
}

case class AerospikeBinProto[T, X](name: String, f: (T => X), converter: AerospikeValueConverter[X]) {
  def apply(o: T) = AerospikeBin(name, f(o))(converter)
}

object AerospikeBin {

  def apply[T <: Any](name: String, value: T)(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] = {
    AerospikeBin(name, converter.toAsV(value), converter)
  }

  def apply[T <: Any](name: String, value: AerospikeList[T])(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] = {
    implicit val listConverter = AerospikeValue.listConverter[T]
    AerospikeBin(name, value, converter)
  }

  def apply[T1 <: Any, T2 <: Any](name: String, value: AerospikeMap[T1, T2])(implicit converter1: AerospikeValueConverter[T1],
    converter2: AerospikeValueConverter[T2]): AerospikeBin[(T1, T2)] = {
    implicit val mapConverter = new AerospikeValue.AerospikeTupleConverter[T1, T2]
    AerospikeBin(name, value, mapConverter)
  }

  def apply[T <: Any](tuple: (String, Object),
    converter: AerospikeValueConverter[T]): AerospikeBin[T] =
    AerospikeBin(tuple._1, AerospikeValue(Value.get(tuple._2))(converter), converter)

  def apply[T <: Any](bin: Bin)(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] = {
    AerospikeBin(bin.name, converter.fromValue(bin.value), converter)
  }

}
