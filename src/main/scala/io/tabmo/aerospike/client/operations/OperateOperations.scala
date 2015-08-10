package io.tabmo.aerospike.client.operations

import scala.concurrent.{ExecutionContext, Future}

import com.aerospike.client.policy.WritePolicy

import io.tabmo.aerospike.client.ReactiveAerospikeClient
import io.tabmo.aerospike.data.AerospikeOperations._
import io.tabmo.aerospike.data.{AerospikeKey, AerospikeOperations, AerospikeRecord}
import io.tabmo.aerospike.listener.AerospikeReadListener

trait OperateOperations {
  self: ReactiveAerospikeClient =>

  def operate[K](key: AerospikeKey[K], operations: AerospikeOperations*)
    (policy: Option[WritePolicy] = None)
      (implicit ec: ExecutionContext): Future[Option[AerospikeRecord]] = {

    logger.debug(s"{$key} OPERATE ${operations.mkString(", ")}")

    val listener = new AerospikeReadListener()
    asyncClient.operate(policy.orNull, listener, key.inner, operations.map(_.toOperation): _*)
    val result = listener.result

    operations.last match {
      case _: get | `getAll` =>
        result.map(Option.apply)
      case _ =>
        result.map(_ => None)
    }
  }

}
