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
import eu.unicredit.reactive_aerospike.future.{Future, Promise}
import com.aerospike.client.policy._

abstract class Dao[K <: Any,T <: ModelObj[K]]
				  (client: AerospikeClient)
				  (implicit _keyConverter: AerospikeValueConverter[K]) {
  
  val namespace: String
  
  val setName: String
  
  val objWrite: Seq[AerospikeBinProto[T,_]]
  val objRead: (AerospikeKey[K],AerospikeRecord) => T
  
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
          	objRead(foundKey,kr._2)
      	  } catch {
      	    case _ : Throwable => throw new Exception("Object not found or not deserializable")
      	  }
        case _ => throw new Exception("key is not of the secified type")
      }
  
  def getCreationKey: (T) => AerospikeKey[K] =
    (obj) => AerospikeKey[K](namespace, setName, obj.getDigest)
  
  def create(obj: T): Future[AerospikeKey[K]] = {
    client.put(
        getCreationKey(obj),
        fromObjToBinSeq(obj))(defaultCreateWritePolicy)
  }
  
  def read(key: AerospikeKey[K]): Future[T] = {
    client.get(key, objBindings)(defaultReadPolicy).map(kr =>
      getObjFromKeyRecord(kr)
    )
  }

  def update(key: AerospikeKey[K], obj: T): Future[AerospikeKey[K]] = {
    client.put(key, fromObjToBinSeq(obj))(defaultUpdateWritePolicy)
  }
  
  def delete(key: AerospikeKey[K]): Future[AerospikeKey[K]] = {
    client.delete(key)(defaultDeleteWritePolicy).map(akb =>
      if (akb._2) akb._1
      else throw new Exception(s"Object with key $key does not exists")
    )
  }
  
  def findOn(field: String, value: String): Future[Seq[T]] = 
    findOn(AerospikeBin(field, value))
  def findOn(field: String, value: Long): Future[Seq[T]] = 
    findOn(AerospikeBin(field, value))
  private def findOn(bin : AerospikeBin[_]): Future[Seq[T]] =
    objBindings.getStub.find(b => b._1 == bin.name) match {
      case None =>
        throw new Exception("Cannot find selected field")
      case Some(proto) =>
        client.queryEqual(AerospikeKey(namespace, setName, ""), objBindings, bin).map(krs =>
          krs.map(kr =>
        	getObjFromKeyRecord(kr)
          )
        )    
    }
  
  def findWithin(field: String, minValue: Long, maxValue: Long): Future[Seq[T]] = 
    objBindings.getStub.find(b => b._1 == field) match {
      case None => throw new Exception("Cannot find selected field")
      case Some(proto) =>
        client.queryRange(AerospikeKey(namespace, setName, ""), objBindings, field, minValue, maxValue).map(krs =>
          krs.map(kr =>
        	getObjFromKeyRecord(kr)
          )
        )
    }

}

abstract class OriginalKeyDao[K <: Any,T <: OriginalKeyModelObj[K]]
				  (client: AerospikeClient)
				  (implicit _keyConverter: AerospikeValueConverter[K]) extends Dao[K,T](client) {
  
  val keyConverter = _keyConverter
  
  private def localKey(key: K): AerospikeKey[K] =
    AerospikeKey(namespace,setName, keyConverter.toAsV(key), keyConverter)
    
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
  
  def read(key: K): Future[T] = {
    client.get(localKey(key), objBindings)(defaultReadPolicy).map(kr =>
      getObjFromKeyRecord(kr)
    )
  }
  
  def update(key: K, obj: T): Future[AerospikeKey[K]] = {
    client.put(localKey(key), fromObjToBinSeq(obj))(defaultUpdateWritePolicy)
  }
  
  def delete(key: K): Future[AerospikeKey[K]] = {
    client.delete(localKey(key))(defaultDeleteWritePolicy).map(akb =>
      if (akb._2) akb._1
      else throw new Exception(s"Object with key $key does not exists")
    )
  }
  
}