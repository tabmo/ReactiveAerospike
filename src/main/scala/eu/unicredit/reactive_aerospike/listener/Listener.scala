package eu.unicredit.reactive_aerospike.listener

import com.aerospike.client.{AerospikeException, Key,Record}
import com.aerospike.client.listener.{WriteListener, RecordListener}
import scala.concurrent.Promise
import eu.unicredit.reactive_aerospike.data.{AerospikeKey, AerospikeValue, AerospikeRecord}
import AerospikeRecord.AerospikeRecordConverter
import AerospikeValue.AerospikeValueConverter
import scala.language.existentials

trait Listener[T <: CommandResult] {
  val promise: Promise[T] = Promise[T]
  val result = promise.future
}

class CommandResult() {}
case class AerospikeWriteReturn[T <: Any](key: AerospikeKey[T]) extends CommandResult
//case class AerospikeDeleteReturn(key: Key, existed: Boolean) extends CommandResult 
//case class AerospikeExistsReturn(key: Key, existed: Boolean) extends CommandResult
case class AerospikeReadReturn[AR <: AerospikeRecord](
		key: AerospikeKey[_], record: AR)
		(implicit recordReader: AerospikeRecordConverter[AR]) extends CommandResult


case class AerospikeWriteListener[T <: Any]()(implicit converter: AerospikeValueConverter[T]) 
				extends WriteListener 
				with Listener[AerospikeWriteReturn[T]] {
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

case class AerospikeReadListener[AR <: AerospikeRecord]()
			(implicit
			    keyConverter: AerospikeValueConverter[_],
			    converter: AerospikeRecordConverter[AR]) 
				extends RecordListener 
				with Listener[AerospikeReadReturn[AR]] {
  
	def onSuccess(key: Key, record: Record) = {
	  AerospikeKey(key)
  	  promise.success(
  	      AerospikeReadReturn(
  			  AerospikeKey(key), AerospikeRecord(record)))
  	}
	
	def onFailure(exception: AerospikeException) = {
  	  promise.failure(exception)
	}
}
