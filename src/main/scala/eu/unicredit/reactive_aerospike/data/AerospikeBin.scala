package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.{Bin, Value}
import AerospikeValue.{AerospikeValueConverter, AerospikeList, AerospikeMap}

case class AerospikeBin[T <: Any]
				(name: String, value: AerospikeValue[T],
				converter: AerospikeValueConverter[T]) {
  
  val inner = new Bin(name, value.inner)

  def toRecordValue = (name -> value.inner.getObject)
  
}

object AerospikeBin {

  def apply[T <: Any](name: String, value: T)
  	(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] = {
    AerospikeBin(name, converter.toAsV(value), converter)
  }

  def apply[T <: Any](name: String, value: AerospikeList[T])
  	(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T] = {
    implicit val listConverter = AerospikeValue.listReader[T]
    AerospikeBin(name, value, converter)
  }
  
  def apply[T1 <: Any,T2 <: Any](name: String, value: AerospikeMap[T1,T2])
  	(implicit converter1: AerospikeValueConverter[T1],
  			  converter2: AerospikeValueConverter[T2]): AerospikeBin[(T1,T2)] = {
    implicit val mapConverter = new AerospikeValue.AerospikeTupleReader[T1,T2]
    AerospikeBin(name, value, mapConverter)
  }
  
 
  def apply[T <: Any](tuple: (String, Object),
		  converter: AerospikeValueConverter[T]): AerospikeBin[T] =
		  AerospikeBin(tuple._1, AerospikeValue(Value.get(tuple._2))(converter), converter)		  
   
  def apply[T <: Any](bin: Bin)
  	(implicit converter: AerospikeValueConverter[T]): AerospikeBin[T]= {
    AerospikeBin(bin.name, converter.fromValue(bin.value), converter)    
  }
  
}