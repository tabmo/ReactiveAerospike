/*
* Copyright 2014 UniCredit S.p.A.
* Copyright 2014 Tabmo.io
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
package io.tabmo.aerospike.client

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

import com.aerospike.client.{Language, AerospikeException, Host}
import com.aerospike.client.async.{AsyncClient, AsyncClientPolicy}
import com.aerospike.client.policy._
import com.aerospike.client.query.{IndexType, Filter, Statement}

import io.tabmo.aerospike.data._
import io.tabmo.aerospike.listener._

object ReactiveAerospikeClient {

  def apply(hostname: String, port: Int)(implicit policy: AsyncClientPolicy = new AsyncClientPolicy()): ReactiveAerospikeClient =
    new ReactiveAerospikeClient(new AsyncClient(policy, hostname, port))

  def apply(policy: AsyncClientPolicy, host: Host, otherHosts: Host*): ReactiveAerospikeClient =
    new ReactiveAerospikeClient(new AsyncClient(policy, host +: otherHosts: _*))

  def apply(policy: AsyncClientPolicy, hostports: String*): ReactiveAerospikeClient = {
    val hosts = hostports.map { hostname =>
      hostname.split(":") match {
        case Array(host) => new Host(host, 3000)
        case Array(host, port) => new Host(host, port.toInt)
        case _ => throw new IllegalArgumentException(s"Invalid hostname provided ($hostname). It must be in the format `host` or `host:port`")
      }
    }
    new ReactiveAerospikeClient(new AsyncClient(policy, hosts: _*))
  }
}

class ReactiveAerospikeClient(val asyncClient: AsyncClient)(implicit policy: AsyncClientPolicy = new AsyncClientPolicy()) {

  def checkConnection() = {
    if (!asyncClient.isConnected) throw new AerospikeException("AerospikeClient not connected to cluster")
  }

  def put[K](key: AerospikeKey[K],  bins: Seq[AerospikeBin[_]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
    implicit val converter = key.converter
    val wl = AerospikeWriteListener()
    asyncClient.put(wpolicy, wl, key.inner, bins.map(_.inner): _*)
    wl.result.map(_.key)
  }

  def append[K](key: AerospikeKey[K],
    bins: Seq[AerospikeBin[_]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
    implicit val converter = key.converter
    val wl = AerospikeWriteListener()
    asyncClient.append(wpolicy, wl, key.inner, bins.map(_.inner): _*)
    wl.result.map(_.key)
  }

  def prepend[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[_]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
    implicit val converter = key.converter
    val wl = AerospikeWriteListener()
    asyncClient.prepend(wpolicy, wl, key.inner, bins.map(_.inner): _*)
    wl.result.map(_.key)
  }

  def delete[K](key: AerospikeKey[K])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[(AerospikeKey[K], Boolean)] = {
    implicit val converter = key.converter
    val dl = AerospikeDeleteListener()
    asyncClient.delete(wpolicy, dl, key.inner)
    dl.result.map(_.key_existed)
  }

  def touch[K](key: AerospikeKey[K])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
    implicit val converter = key.converter
    val wl = AerospikeWriteListener()
    asyncClient.touch(wpolicy, wl, key.inner)
    wl.result.map(_.key)
  }

  def exists[K](key: AerospikeKey[K])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[(AerospikeKey[K], Boolean)] = {
    implicit val converter = key.converter
    val el = AerospikeExistsListener()
    asyncClient.exists(wpolicy, el, key.inner)
    el.result.map(_.key_existed)
  }

  def add[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[Long]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
    implicit val converter = key.converter
    val wl = AerospikeWriteListener()
    asyncClient.add(wpolicy, wl, key.inner, bins.map(_.inner): _*)
    wl.result.map(_.key)
  }

  def get(key: AerospikeKey[_], recordReader: AerospikeRecordReader)(implicit rpolicy: Policy = policy.readPolicyDefault): Future[(AerospikeKey[_], AerospikeRecord)] = {
    implicit val keyConverter = key.converter
    val rl = AerospikeReadListener(recordReader)
    asyncClient.get(rpolicy, rl, key.inner)
    rl.result.map(x => x.key_record)
  }

  def getBins(key: AerospikeKey[_], binNames: Seq[String], recordReader: AerospikeRecordReader)(implicit rpolicy: Policy = policy.readPolicyDefault): Future[(AerospikeKey[_], AerospikeRecord)] = {
    implicit val keyConverter = key.converter
    val rl = AerospikeReadListener(recordReader)
    asyncClient.get(rpolicy, rl, key.inner, binNames: _*)
    rl.result.map(x => x.key_record)
  }

  def getHeader(key: AerospikeKey[_], recordReader: AerospikeRecordReader)(implicit rpolicy: Policy = policy.readPolicyDefault): Future[(AerospikeKey[_], AerospikeRecord)] = {
    implicit val keyConverter = key.converter
    val rl = AerospikeReadListener(recordReader)
    asyncClient.getHeader(rpolicy, rl, key.inner)
    rl.result.map(x => x.key_record)
  }

  def getMulti[T](keys: Seq[AerospikeKey[T]], recordReader: AerospikeRecordReader)(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    implicit val keyConverter = keys.head.converter
    val rl = AerospikeMultipleReadListener(recordReader)
    asyncClient.get(bpolicy, rl, keys.map(_.inner).toArray)
    rl.result.map(x => x.key_records)
  }

  def getMultiDifferent(keys_record_readers: Seq[(AerospikeKey[_], AerospikeRecordReader)])(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    val rl = AerospikeMultipleDifferentReadListener(keys_record_readers.map(x => (x._1.converter, x._2)))
    asyncClient.get(bpolicy, rl, keys_record_readers.map(_._1.inner).toArray)
    rl.result.map(x => x.key_records)
  }

  def getMultiBins[T](keys: Seq[AerospikeKey[T]], binNames: Seq[String], recordReader: AerospikeRecordReader)(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    implicit val keyConverter = keys.head.converter
    val rl = AerospikeMultipleReadListener(recordReader)
    asyncClient.get(bpolicy, rl, keys.map(_.inner).toArray, binNames: _*)
    rl.result.map(x => x.key_records)
  }

  def getMultiHeader[T](keys: Seq[AerospikeKey[T]], recordReader: AerospikeRecordReader)(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    implicit val keyConverter = keys.head.converter
    val rl = AerospikeMultipleReadListener(recordReader)
    asyncClient.getHeader(bpolicy, rl, keys.map(_.inner).toArray)
    rl.result.map(x => x.key_records)
  }

  def queryEqual[T](key_stub: AerospikeKey[T], recordReader: AerospikeRecordReader, filter: AerospikeBin[_])(implicit qpolicy: QueryPolicy = policy.queryPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    val statement = new Statement()
    statement.setNamespace(key_stub.namespace)
    key_stub.setName.foreach(statement.setSetName)

    statement.setFilters(
      Filter.equal(filter.name, filter.value.inner.toString)
    )

    implicit val keyConverter = key_stub.converter
    val sl = AerospikeSequenceReadListener[T](recordReader)
    asyncClient.query(qpolicy, sl, statement)
    sl.result.map(x => x.key_records)
  }

  def queryRange[T](key_stub: AerospikeKey[T], recordReader: AerospikeRecordReader, filterBinName: String, rangeMin: Long, rangeMax: Long)(implicit qpolicy: QueryPolicy = policy.queryPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    val statement = new Statement()
    statement.setNamespace(key_stub.namespace)
    key_stub.setName.foreach(statement.setSetName)

    statement.setFilters(
      Filter.range(filterBinName, rangeMin, rangeMax)
    )

    implicit val keyConverter = key_stub.converter
    val sl = AerospikeSequenceReadListener[T](recordReader)
    asyncClient.query(qpolicy, sl, statement)
    sl.result.map(x => x.key_records)
  }

  def queryEqualAggregateMap[T](namespace: String, set: String, filter: AerospikeBin[_],
    classloader: ClassLoader, resourcePath: String, packageName: String, functionName: String, functionArgs: AerospikeValue[_]*)(implicit qpolicy: QueryPolicy = policy.queryPolicyDefault): Future[Seq[Map[String, _]]] = {

    val statement = new Statement()
    statement.setNamespace(namespace)
    statement.setSetName(set)
    statement.setAggregateFunction(classloader, resourcePath, packageName, functionName, functionArgs.map(_.inner): _*)

    statement.setFilters(
        Filter.equal(filter.name, filter.value.inner.toString)
    )

    Future {
      val result = asyncClient.queryAggregate(qpolicy, statement)
      result.iterator().asScala.toList.map {
        case r: java.util.HashMap[_, _] => r.asScala.toMap.asInstanceOf[Map[String, _]]
        case r => throw new IllegalArgumentException(s"query result is of type ${r.getClass}, expecting HashMap")
      }
    }
  }

  def registerUDF(resourceLoader: ClassLoader, resourcePath: String, serverPath: String, language: Language = Language.LUA)(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[Unit] = {
    Future {
      val task = asyncClient.register(wpolicy, resourceLoader, resourcePath, serverPath, language)
      while (!task.isDone) task.waitTillComplete(500)
    }
  }

  def registerUDFFromPath(resourcePath: String, serverPath: String, language: Language = Language.LUA)(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[Unit] = {
    Future {
      val task = asyncClient.register(wpolicy, resourcePath, serverPath, language)
      while (!task.isDone) task.waitTillComplete(500)
    }
  }

  def removeUDF(serverPath: String)(implicit ipolicy: InfoPolicy = policy.infoPolicyDefault) = {
    Future {
      asyncClient.removeUdf(ipolicy, serverPath)
    }
  }

  def createIndex(namespace: String, setName: String, binName: String, indexType: IndexType)(implicit rpolicy: Policy = policy.asyncReadPolicyDefault): Future[String] = {
    Future {
      val indexName = s"${namespace}_${setName}_$binName"
      val task = asyncClient.createIndex(rpolicy, namespace, setName, indexName, binName, indexType)
      while (!task.isDone) task.waitTillComplete(500)
      indexName
    }
  }

  def dropIndex(namespace: String, setName: String, indexName: String)(implicit rpolicy: Policy = policy.asyncReadPolicyDefault): Future[Unit] = {
    Future {
      asyncClient.dropIndex(rpolicy, namespace, setName, indexName)
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
