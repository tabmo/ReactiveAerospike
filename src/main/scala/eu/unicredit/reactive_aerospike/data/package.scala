package eu.unicredit.reactive_aerospike

import com.aerospike.client.{ Bin, Key }

package object data {

  //Aerospike Value implicits
  implicit def fromNullToAS(n: Null) =
    AerospikeValue(n)

  implicit def fromStringToAS(s: String) =
    AerospikeValue(s)

  implicit def fromIntToAS(i: Int) =
    AerospikeValue(i)
  implicit def fromLongToAS(l: Long) =
    AerospikeValue(l)

  //From and to Bin
  implicit def fromTupleToBin[T <: Any](t: (String, T)): AerospikeBin[AerospikeValue[_]] =
    AerospikeBin(t._1, AerospikeValue(t._2))

  implicit def fromABToBin(ab: AerospikeBin[AerospikeValue[_]]): Bin =
    ab.inner

  //from and to aerospike Key
  implicit def fromAKToK(ak: AerospikeKey[AerospikeValue[_]]): Key =
    ak.inner

  //from seq to Array of Bins
  implicit def fromSeqToArr(in: Seq[AerospikeBin[AerospikeValue[_]]]): Array[AerospikeBin[AerospikeValue[_]]] =
    in.toArray
}