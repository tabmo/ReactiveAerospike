package io.tabmo.aerospike.client.operations

import scala.concurrent.{ExecutionContext, Future}

import com.aerospike.client.Language
import com.aerospike.client.policy.{InfoPolicy, WritePolicy}

import io.tabmo.aerospike.client.ReactiveAerospikeClient

trait UdfOperations {
  self: ReactiveAerospikeClient =>

  def registerUDF(
    resourceLoader: ClassLoader,
    resourcePath: String,
    serverPath: String,
    language: Language = Language.LUA,
    policy: Option[WritePolicy] = None)
      (implicit ec: ExecutionContext): Future[Unit] = {

    logger.timing(s"REGISTER UDF $resourcePath:$serverPath") {
      Future {
        val task = asyncClient.register(policy.orNull, resourceLoader, resourcePath, serverPath, language)
        while (!task.isDone) task.waitTillComplete(500)
      }
    }

  }

  def registerUDFFromPath(
    resourcePath: String,
    serverPath: String,
    language: Language = Language.LUA,
    policy: Option[WritePolicy] = None)
      (implicit ec: ExecutionContext): Future[Unit] = {

    logger.timing(s"REGISTER UDF $resourcePath:$serverPath") {
      Future {
        val task = asyncClient.register(policy.orNull, resourcePath, serverPath, language)
        while (!task.isDone) task.waitTillComplete(500)
      }
    }
  }

  def removeUDF(
    serverPath: String,
    policy: Option[InfoPolicy] = None)
      (implicit executionContext: ExecutionContext): Future[Unit] = {

    logger.timing(s"REMOVE UDF $serverPath") {
      Future {
        asyncClient.removeUdf(policy.orNull, serverPath)
      }
    }
  }

  /*def executeUDF[T](key_stub: AerospikeKey[T], packageName: String, functionName: String, args: AerospikeValue[_]*)(implicit wpolicy: WritePolicy = policy.asyncWritePolicyDefault): Future[_] = {
    Future {
      client
        .execute(wpolicy, key_stub.inner, packageName, functionName, args.map(_.inner): _*)
    }.map {
      case r: java.util.ArrayList[_] => r.asScala.toList
      case r: java.util.HashMap[_, _] => r.asScala.toMap
      case r => r
    }
  }

  def executeUDF[T](statement: Statement, packageName: String, functionName: String, args: AerospikeValue[_]*)(implicit wpolicy: WritePolicy = policy.asyncWritePolicyDefault): Future[_] = {
    Future {
      val task = client
        .execute(wpolicy, statement, packageName, functionName, args.map(_.inner): _*)
      while (task.isDone) task.waitTillComplete(500)
    }
  }*/

}
