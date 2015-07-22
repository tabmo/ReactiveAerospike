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

package eu.unicredit.reactive_aerospike.listener

import scala.util.control.NonFatal

import com.aerospike.client.{ AerospikeException, Key, Record }
import com.aerospike.client.listener.{
  WriteListener,
  RecordListener,
  DeleteListener,
  ExistsListener,
  RecordArrayListener,
  RecordSequenceListener
}
import eu.unicredit.reactive_aerospike.data._
import AerospikeValue.AerospikeValueConverter
import scala.collection.immutable.Stream._
import scala.concurrent.Future

class Listener[T <: CommandResult] {
  val promise = scala.concurrent.Promise.apply[T]()
  val result: Future[T] = promise.future
}

trait CommandResult

case class AerospikeWriteReturn[T <: Any](key: AerospikeKey[T]) extends CommandResult
case class AerospikeDeleteReturn[T <: Any](key_existed: (AerospikeKey[T], Boolean)) extends CommandResult
case class AerospikeExistsReturn[T <: Any](key_existed: (AerospikeKey[T], Boolean)) extends CommandResult
case class AerospikeReadReturn[T <: Any](key_record: (AerospikeKey[_], AerospikeRecord))(implicit recordReader: AerospikeRecordReader) extends CommandResult
case class AerospikeMultipleReadReturn[T <: Any](key_records: Seq[(AerospikeKey[_], AerospikeRecord)])(implicit recordReader: Seq[AerospikeRecordReader]) extends CommandResult

case class AerospikeWriteListener[T <: Any]()(implicit converter: AerospikeValueConverter[T])
  extends Listener[AerospikeWriteReturn[T]] with WriteListener {

  override def onSuccess(key: Key) = {
    promise.success(AerospikeWriteReturn(AerospikeKey(key)))
  }

  override def onFailure(exception: AerospikeException) = {
    promise.failure(exception)
  }
}

case class AerospikeDeleteListener[T <: Any]()(implicit converter: AerospikeValueConverter[T])
  extends Listener[AerospikeDeleteReturn[T]] with DeleteListener {

  override def onSuccess(key: Key, existed: Boolean) = {
    promise.success(AerospikeDeleteReturn((AerospikeKey(key), existed)))
  }

  override def onFailure(exception: AerospikeException) = {
    promise.failure(exception)
  }
}

case class AerospikeExistsListener[T <: Any]()(implicit converter: AerospikeValueConverter[T])
  extends Listener[AerospikeDeleteReturn[T]] with ExistsListener {
  override def onSuccess(key: Key, existed: Boolean) = {
    promise.success(AerospikeDeleteReturn((AerospikeKey(key), existed)))
  }

  override def onFailure(exception: AerospikeException) = {
    promise.failure(exception)
  }
}

case class AerospikeReadListener[T <: Any](converter: AerospikeRecordReader)(implicit keyConverter: AerospikeValueConverter[_])
  extends Listener[AerospikeReadReturn[T]] with RecordListener {
  implicit val conv = converter

  override def onSuccess(key: Key, record: Record) = {
    if (record == null)
      promise.failure(new AerospikeException(s"Key not found: $key"))
    else {
      try {
        val ar = AerospikeRecord(record)
        promise.success(AerospikeReadReturn((AerospikeKey(key), ar)))
      } catch {
        case NonFatal(err) =>
          promise.failure(new AerospikeException(err))
      }
    }
  }

  override def onFailure(exception: AerospikeException) = {
    promise.failure(exception)
  }
}

case class AerospikeMultipleReadListener[T <: Any](converter: AerospikeRecordReader)(implicit keyConverter: AerospikeValueConverter[_])
  extends Listener[AerospikeMultipleReadReturn[T]] with RecordArrayListener {
  implicit val conv = converter

  override def onSuccess(keys: Array[Key], records: Array[Record]) = {
    try {
      val results = keys.zip(records).map { case (key, record) =>
        (AerospikeKey(key), AerospikeRecord(record))
      }
      implicit val readers = keys.map(_ => converter).toSeq
      promise.success(AerospikeMultipleReadReturn(results))
    } catch {
      case NonFatal(err) =>
        promise.failure(new AerospikeException(err))
    }
  }

  override def onFailure(exception: AerospikeException) = {
    promise.failure(exception)
  }
}

case class AerospikeMultipleDifferentReadListener[T <: Any](keys_converters: Seq[(AerospikeValueConverter[_], AerospikeRecordReader)])
  extends Listener[AerospikeMultipleReadReturn[T]] with RecordArrayListener {
  override def onSuccess(keys: Array[Key], records: Array[Record]) = {
    try {
      val results = for {
        i_kr <- keys.zip(records).zipWithIndex
        keyConverter = try { Some(keys_converters(i_kr._2)._1) } catch { case NonFatal(_) => None }
        recordConverter = try { Some(keys_converters(i_kr._2)._2) } catch { case NonFatal(_) => None }
        if keyConverter.isDefined && recordConverter.isDefined
      } yield {
        (AerospikeKey(i_kr._1._1)(keys_converters(i_kr._2)._1),
          AerospikeRecord(i_kr._1._2)(keys_converters(i_kr._2)._2))
      }
      implicit val readers = keys_converters.map(_._2)
      promise.success(AerospikeMultipleReadReturn(results.toSeq))
    } catch {
      case NonFatal(err) =>
        promise.failure(new AerospikeException(err))
    }
  }

  override def onFailure(exception: AerospikeException) = {
    promise.failure(exception)
  }
}

case class AerospikeSequenceReadListener[T <: Any](converter: AerospikeRecordReader)(implicit keyConverter: AerospikeValueConverter[T])
  extends Listener[AerospikeMultipleReadReturn[T]] with RecordSequenceListener {
  implicit val conv = converter

  val stream: StreamBuilder[(AerospikeKey[T], AerospikeRecord)] = new StreamBuilder()

  def onRecord(key: Key, record: Record) = {
    val toAdd =
      try
        Some((AerospikeKey(key), AerospikeRecord(record)))
      catch {
        case NonFatal(err) => err.printStackTrace(); None
      }

    toAdd.map(stream += _)
  }

  override def onSuccess() = {
    val result = stream.result().toSeq
    val readers = for (i <- 0 to result.length) yield converter

    promise.success(AerospikeMultipleReadReturn(result)(readers))
  }

  override def onFailure(exception: AerospikeException) = {
    promise.failure(exception)
  }
}
