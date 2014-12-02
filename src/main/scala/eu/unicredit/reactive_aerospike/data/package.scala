package eu.unicredit.reactive_aerospike

import com.aerospike.client.{ Bin, Key }
import data.AerospikeValue.AerospikeValueConverter

package object data {

  //Aerospike Value implicits
  
  implicit def fromNullToAS(n: Null) =
    AerospikeValue.AerospikeNull()

  implicit def fromStringToAS(s: String) =
    AerospikeValue(s)(AerospikeValue.AerospikeStringRW)

  implicit def fromIntToAS(i: Int) =
    AerospikeValue(i.toLong)(AerospikeValue.AerospikeLongRW)
  implicit def fromLongToAS(l: Long) =
    AerospikeValue(l)(AerospikeValue.AerospikeLongRW)

  //From and to Bin
  implicit def fromTupleToBin[T <: Any](t: (String, T))
  		(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] =
    AerospikeBin(t._1, converter.toAsV(t._2), converter)
  
  implicit def fromABToBin[T <: Any](ab: AerospikeBin[AerospikeValue[T]]): Bin =
    ab.inner

  //from and to aerospike Key
  implicit def fromAKToK[T <: Any](ak: AerospikeKey[AerospikeValue[T]]): Key =
    ak.inner

  //from seq to Array of Bins
  implicit def fromSeqToArr[T <: Any](in: Seq[AerospikeBin[AerospikeValue[T]]]): 
	  	Array[AerospikeBin[AerospikeValue[T]]] =
    in.toArray
    
}