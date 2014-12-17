package eu.unicredit.reactive_aerospike.model

import eu.unicredit.reactive_aerospike.client.AerospikeClient
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.data.AerospikeValue._
import eu.unicredit.reactive_aerospike.future.{Future, Promise}
import com.aerospike.client.policy._

trait Dao[K <: Any,T <: ModelObj[K]] {
  
  val client: AerospikeClient

  //Da sistemare...
  implicit val executionContext: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val namespace: String
  
  val set: String
  
  val keyConverter: AerospikeValueConverter[K]
   
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
  
  private def localKey(key: K) =
    AerospikeKey(namespace,set, keyConverter.toAsV(key), keyConverter)
    
  private def localKey(key: AerospikeValue[K]) =
    AerospikeKey(namespace,set, key, keyConverter)    
  
  val defaultCreateWritePolicy = {
    val wp = new WritePolicy
    wp.recordExistsAction = RecordExistsAction.CREATE_ONLY
    wp.generation = 0
    wp.expiration = -1
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
    
  def create(obj: T): Future[K] = {
    client.put(localKey(obj.getKey), fromObjToBinSeq(obj))(defaultCreateWritePolicy).map(ak =>
      ak.userKey
    )
  }
  
  def read(key: K): Future[T] = {
    client.get(localKey(key), objBindings)(defaultReadPolicy).map(kr =>
      kr._1 match {
        case foundKey: AerospikeKey[K] =>
          try {
          	objRead(foundKey,kr._2)
      	  } catch {
      	    case _ : Throwable => throw new Exception("Object not found or not deserializable")
      	  }
        case _ => throw new Exception("key is not of the secified type")
      }
    )
  }

  def update(key: K, obj: T): Future[K] = {
    client.put(localKey(key), fromObjToBinSeq(obj))(defaultUpdateWritePolicy).map(ak =>
      ak.userKey 
    )
  }
  
  def delete(key: K): Future[K] = {
    client.delete(localKey(key))(defaultDeleteWritePolicy).map(akb =>
      if (akb._2) akb._1.userKey 
      else throw new Exception(s"Object with key $key does not exists")
    )
  }

}