package eu.unicredit.reactive_aerospike.listener

import com.aerospike.client.{AerospikeException, Key,Record}
import com.aerospike.client.listener.{WriteListener, RecordListener}
import scala.concurrent.Promise
import eu.unicredit.reactive_aerospike.data._
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
case class AerospikeReadReturn[T <: Any](
		key: AerospikeKey[_], record: AerospikeRecord)
		(implicit recordReader: AerospikeRecordReader) extends CommandResult


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

case class AerospikeReadListener[T <: Any](converter: AerospikeRecordReader)
			(implicit
			    keyConverter: AerospikeValueConverter[_]) 
				extends RecordListener 
				with Listener[AerospikeReadReturn[T]] {
	implicit val conv = converter 
  
	def onSuccess(key: Key, record: Record) = {
	  if (record==null)
	    	promise.failure(new AerospikeException(s"Selected key: $key not found"))
	  else {
		try {
		  val ar =
			AerospikeRecord(record)
		  promise.success(
			AerospikeReadReturn(
  			  AerospikeKey(key), ar))
		} catch {
	    	case err: Throwable => 
	    	  err.printStackTrace();
	    	  promise.failure(new AerospikeException(s"Cannot deserialize record for key: $key"))
		}
  	  
	  }
  	}
	
	def onFailure(exception: AerospikeException) = {
  	  promise.failure(exception)
	}
}
