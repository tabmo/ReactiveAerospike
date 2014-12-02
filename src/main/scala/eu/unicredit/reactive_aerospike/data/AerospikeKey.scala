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

//  implicit def fromAKToK(ak: AerospikeKey[_]): Key =
//    ak.inner

}