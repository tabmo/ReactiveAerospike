package eu.unicredit.reactive_aerospike

import com.aerospike.client.{ Bin, Key }
import data.AerospikeValue._

package object data {

  //Aerospike Value implicits
  
  implicit def fromNullToAS(n: Null) =
    AerospikeValue.AerospikeNull()

  implicit def fromStringToAS(s: String) =
    AerospikeString(s)

  implicit def fromIntToAS(i: Int) =
    AerospikeInt(i.toInt)
  implicit def fromLongToAS(l: Long) =
    AerospikeLong(l)

  //From and to Bin
  /*
  implicit def fromTupleToBin[T <: Any](t: (String, T))
  		(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] =
    AerospikeBin(t._1, converter.toAsV(t._2), converter)
  */  
  implicit def fromTupleToBin[T <: Any](t: (String, AerospikeValue[T]))
  		(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] =
    AerospikeBin(t._1, t._2, converter)
 
  
  implicit def fromABToBin[T <: Any](ab: AerospikeBin[AerospikeValue[T]]): Bin =
    ab.inner
    
  implicit def fromGenToInstanceBin[T <: Any](value: AerospikeValue[_]): AerospikeValue[T] =
    value match {
    	case t: AerospikeValue[T] =>
      		t
    	case _ =>
      		throw new Exception("Cannot retrieve requested bin! Please ask Andrea  a fix for that")
  	}

  //from and to aerospike Key
  implicit def fromAKToK[T <: Any](ak: AerospikeKey[AerospikeValue[T]]): Key =
    ak.inner

  //from seq to Array of Bins
  implicit def fromSeqToArr[T <: Any](in: Seq[AerospikeBin[AerospikeValue[T]]]): 
	  	Array[AerospikeBin[AerospikeValue[T]]] =
    in.toArray
    
}