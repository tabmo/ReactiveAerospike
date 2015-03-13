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

package eu.unicredit.reactive_aerospike.model

import eu.unicredit.reactive_aerospike.client.AerospikeClient
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.data.AerospikeValue._
import eu.unicredit.reactive_aerospike.future.{ Future, Promise }
import com.aerospike.client.policy._

abstract class Dao[K <: Any, T <: Any](implicit _keyConverter: AerospikeValueConverter[K]) {

  val namespace: String

  val setName: String

  def getKeyDigest(obj: T): Array[Byte]

  val objWrite: Seq[AerospikeBinProto[T, _]]
  val objRead: (AerospikeKey[K], AerospikeRecord) => T

  def objBindingsMap(s: Seq[(String, AerospikeValueConverter[_])]) =
    s.toMap

  lazy val objBindings: AerospikeRecordReader =
    objBindingsMap(objWrite.map(x => (x.name, x.converter)))

  def fromObjToBinSeq(obj: T): Seq[AerospikeBin[_]] =
    objWrite.map(c =>
      c(obj)
    )

  val defaultCreateWritePolicy = {
    val wp = new WritePolicy
    wp.recordExistsAction = RecordExistsAction.CREATE_ONLY
    wp.generation = 0
    wp.expiration = -1
    wp.sendKey = false
    wp
  }

  val defaultReadPolicy = {
    val rp = new Policy
    rp.priority = Priority.DEFAULT
    rp.timeout = 0 //No timeout?
    rp.maxRetries = 3
    rp.sleepBetweenRetries = 100
    rp
  }

  val defaultUpdateWritePolicy = {
    val wp = new WritePolicy
    wp.recordExistsAction = RecordExistsAction.UPDATE_ONLY
    wp.generation = 0
    wp.expiration = -1
    wp
  }

  val defaultDeleteWritePolicy = {
    val wp = new WritePolicy
    wp.recordExistsAction = RecordExistsAction.UPDATE_ONLY
    wp.generation = 0
    wp.expiration = -1
    wp
  }

  def getObjFromKeyRecord(kr: (AerospikeKey[_], AerospikeRecord)): T =
    kr._1 match {
      case foundKey: AerospikeKey[K] =>
        try {
          objRead(foundKey, kr._2)
        } catch {
          case _: Throwable => throw new Exception("Object not found or not deserializable")
        }
      case _ => throw new Exception("key is not of the secified type")
    }

  def getCreationKey: (T) => AerospikeKey[K] =
    (obj) => AerospikeKey[K](namespace, setName, getKeyDigest(obj))

  def create[F[_]](obj: T)(implicit client: AerospikeClient[F]) = {
    client.getFactory.toBase(
      client.inner_put(
        getCreationKey(obj),
        fromObjToBinSeq(obj))(defaultCreateWritePolicy)
    )
  }

  def read[F[_]](key: AerospikeKey[K])(implicit client: AerospikeClient[F]) = {
    client.getFactory.toBase(
      client.inner_get(key, objBindings)(defaultReadPolicy).map(kr =>
        getObjFromKeyRecord(kr)
      ))
  }

  def update[F[_]](key: AerospikeKey[K], obj: T)(implicit client: AerospikeClient[F]) = {
    client.getFactory.toBase(
      client.inner_put(key, fromObjToBinSeq(obj))(defaultUpdateWritePolicy)
    )
  }

  def delete[F[_]](key: AerospikeKey[K])(implicit client: AerospikeClient[F]) = {
    client.getFactory.toBase(
      client.inner_delete(key)(defaultDeleteWritePolicy).map(akb =>
        if (akb._2) akb._1
        else throw new Exception(s"Object with key $key does not exists")
      ))
  }

  def findOn[F[_]](field: String, value: String)(implicit client: AerospikeClient[F]): F[Seq[T]] =
    findOn(AerospikeBin(field, value))
  def findOn[F[_]](field: String, value: Long)(implicit client: AerospikeClient[F]): F[Seq[T]] =
    findOn(AerospikeBin(field, value))
  private def findOn[F[_]](bin: AerospikeBin[_])(implicit client: AerospikeClient[F]): F[Seq[T]] =
    objBindings.getStub.find(b => b._1 == bin.name) match {
      case None =>
        throw new Exception("Cannot find selected field")
      case Some(proto) =>
        client.getFactory.toBase(
          client.inner_queryEqual(AerospikeKey(namespace, setName, ""), objBindings, bin).map(krs =>
            krs.map(kr =>
              getObjFromKeyRecord(kr)
            )
          ))
    }

  def findWithin[F[T]](field: String, minValue: Long, maxValue: Long)(implicit client: AerospikeClient[F]) = //: F[Seq[T]] =
    objBindings.getStub.find(b => b._1 == field) match {
      case None => throw new Exception("Cannot find selected field")
      case Some(proto) =>
        client.getFactory.toBase(
          client.inner_queryRange(AerospikeKey(namespace, setName, ""), objBindings, field, minValue, maxValue).map(krs =>
            krs.map(kr =>
              getObjFromKeyRecord(kr)
            )
          ))
    }

  implicit val implicitMe: Dao[K, T] = this
}

abstract class DigestDao[K <: Any, T <: ModelObj[K]](implicit _keyConverter: AerospikeValueConverter[K]) extends Dao[K, T] {

  override def getKeyDigest(obj: T): Array[Byte] =
    obj.getDigest

}

abstract class OriginalKeyDao[K <: Any, T <: OriginalKeyModelObj[K]](implicit _keyConverter: AerospikeValueConverter[K]) extends DigestDao[K, T] {

  val keyConverter = _keyConverter

  private def localKey(key: K): AerospikeKey[K] =
    AerospikeKey(namespace, setName, keyConverter.toAsV(key), keyConverter)

  override val defaultCreateWritePolicy = {
    val wp = new WritePolicy
    wp.recordExistsAction = RecordExistsAction.CREATE_ONLY
    wp.generation = 0
    wp.expiration = -1
    wp.sendKey = true
    wp
  }

  override def getCreationKey: (T) => AerospikeKey[K] = {
    (obj) => obj.getKey
  }

  def read[F[_]](key: K)(implicit client: AerospikeClient[F]): F[T] = {
    client.getFactory.toBase(
      client.inner_get(localKey(key), objBindings)(defaultReadPolicy).map(kr =>
        getObjFromKeyRecord(kr)
      ))
  }

  def update[F[_]](key: K, obj: T)(implicit client: AerospikeClient[F]) = { //: F[AerospikeKey[K]] = {
    client.getFactory.toBase(
      client.inner_put(localKey(key), fromObjToBinSeq(obj))(defaultUpdateWritePolicy)
    )
  }

  def delete[F[_]](key: K)(implicit client: AerospikeClient[F]) = { //: F[AerospikeKey[K]] = {
    client.getFactory.toBase(
      client.inner_delete(localKey(key))(defaultDeleteWritePolicy).map(akb =>
        if (akb._2) akb._1
        else throw new Exception(s"Object with key $key does not exists")
      ))
  }

  override implicit val implicitMe: OriginalKeyDao[K, T] = this
}