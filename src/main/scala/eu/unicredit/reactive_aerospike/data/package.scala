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

import com.aerospike.client.{ Bin, Key }
import data.AerospikeValue._

package object data {

  implicit def fromNullToAS(n: Null) = AerospikeValue.AerospikeNull()

  implicit def fromStringToAS(s: String) = AerospikeString(s)

  implicit def fromIntToAS(i: Int) = AerospikeInt(i)
  implicit def fromLongToAS(l: Long) = AerospikeLong(l)
  implicit def fromDoubleToAS(d: Double) = AerospikeDouble(d)

  implicit def fromASVtoValueAS[T](x: AerospikeValue[T]) = x.base

  implicit def fromTupleToBin[T <: Any](t: (String, AerospikeValue[T]))(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] =
    AerospikeBin(t._1, t._2, converter)

  implicit def fromTripletToABP[T, X](t: (String, (T => X), AerospikeValueConverter[X])) =
    AerospikeBinProto(t._1, t._2, t._3)

  implicit def fromTupleToABP[T, X](t: (String, (T => X)))(implicit converter: AerospikeValueConverter[X]) =
    AerospikeBinProto(t._1, t._2, converter)

  implicit def fromABToBin[T <: Any](ab: AerospikeBin[AerospikeValue[T]]): Bin = ab.inner

  implicit def fromGenToInstanceBin[X <: Any](value: AerospikeValue[_]): AerospikeValue[X] = {
    val manif = scala.reflect.ClassManifestFactory.classType[X](value.base.getClass)
    assert(manif.runtimeClass.isInstance(value.base))
    value.asInstanceOf[AerospikeValue[X]]
  }

  //from and to Aerospike Key
  implicit def fromAKToK[T <: Any](ak: AerospikeKey[AerospikeValue[T]]): Key = ak.inner

  //from Seq to Array of Bins
  implicit def fromSeqToArr[T <: Any](in: Seq[AerospikeBin[AerospikeValue[T]]]): Array[AerospikeBin[AerospikeValue[T]]] =
    in.toArray

  //from map to AerospikeRecordReader
  implicit def fromMapToARR(in: Map[String, AerospikeValueConverter[_]]): AerospikeRecordReader =
    new AerospikeRecordReader(in)

}
