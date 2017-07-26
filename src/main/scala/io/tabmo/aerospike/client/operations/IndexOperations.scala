package io.tabmo.aerospike.client.operations

import scala.concurrent.{Future, ExecutionContext}

import com.aerospike.client.policy.Policy
import com.aerospike.client.query.IndexType
import scala.concurrent._

import io.tabmo.aerospike.client.ReactiveAerospikeClient

trait IndexOperations {
  self: ReactiveAerospikeClient =>

  def createIndex(namespace: String, setName: String, binName: String, indexType: IndexType, indexName: Option[String] = None, policy: Option[Policy] = None)
    (implicit ec: ExecutionContext): Future[String] = {

    logger.timing(s"CREATE INDEX $namespace:$setName:$binName ($indexType)") {
      Future {
        blocking {
          val indexNameDefault = indexName.getOrElse(s"${namespace}_${setName}_$binName")
          val task = asyncClient.createIndex(policy.orNull, namespace, setName, indexNameDefault, binName, indexType)
          task.waitTillComplete()
          indexNameDefault
        }
      }
    }
  }

  def dropIndex(namespace: String, setName: String, indexName: String, policy: Option[Policy] = None)
    (implicit ec: ExecutionContext): Future[Unit] = {

    logger.timing(s"DROP INDEX $namespace:$setName $indexName") {
      Future {
        asyncClient.dropIndex(policy.orNull, namespace, setName, indexName)
      }
    }
  }

}
