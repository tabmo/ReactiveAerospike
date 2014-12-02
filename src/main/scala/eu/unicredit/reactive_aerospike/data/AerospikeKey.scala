package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.Key
import scala.language.existentials

case class AerospikeKey[T <: AerospikeValue[_]](
    namespace: String,
    setName: String,
    userKey: T) {

  val inner = new Key(namespace, setName, userKey)

}

object AerospikeKey {

  def apply(namespace: String,
    setName: String,
    userKey: Any): AerospikeKey[AerospikeValue[_]] =
    AerospikeKey(
      namespace,
      setName,
      AerospikeValue(userKey)
    )

  def apply(key: Key): AerospikeKey[AerospikeValue[_]] =
    AerospikeKey(
      key.namespace,
      key.setName,
      AerospikeValue(key.userKey)
      )

//  implicit def fromAKToK(ak: AerospikeKey[_]): Key =
//    ak.inner

}