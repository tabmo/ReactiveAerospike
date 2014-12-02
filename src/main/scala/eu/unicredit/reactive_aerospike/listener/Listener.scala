package eu.unicredit.reactive_aerospike.listener

import com.aerospike.client.AerospikeException
import com.aerospike.client.Key
import com.aerospike.client.listener.WriteListener
import scala.concurrent.Promise
import eu.unicredit.reactive_aerospike.data.{AerospikeKey, AerospikeValue}
import scala.language.existentials

trait Listener[T <: CommandResult] {
  val promise: Promise[T] = Promise[T]
  val result = promise.future
}

class CommandResult() {}
case class AerospikeWriteReturn(key: AerospikeKey[AerospikeValue[_]]) extends CommandResult
case class AerospikeDeleteReturn(key: Key, existed: Boolean) extends CommandResult 
case class AerospikeExistsReturn(key: Key, existed: Boolean) extends CommandResult


case class AerospikeWriteListener() 
				extends WriteListener 
				with Listener[AerospikeWriteReturn] {
  	def onSuccess(key: Key) = {
  	  promise.success(
  	      AerospikeWriteReturn(
  			  AerospikeKey(key)))
  	}
	
	def onFailure(exception: AerospikeException) = {
  	  promise.failure(exception)
	}
}
//case class ExistsListener() extends Listener[ExistsReturn] {}
//case class DeleteListener() extends Listener[DeleteReturn] {}