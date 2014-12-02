package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.Bin

case class AerospikeBin[+T <: AerospikeValue[_]](name: String, value: T) {

  val inner = new Bin(name, value.inner)

  def toRecordValue = (name -> value.inner.getObject)

}

object AerospikeBin {
  
  def apply(name: String, value: Any): AerospikeBin[AerospikeValue[_]] =
		  AerospikeBin(name, AerospikeValue(value))

		  
  def apply(tuple: (String, Any)): AerospikeBin[AerospikeValue[_]] =
		  AerospikeBin(tuple._1, AerospikeValue(tuple._2))
		  
}