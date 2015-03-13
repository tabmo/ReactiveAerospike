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
package eu.unicredit.reactive_aerospike.client

import com.aerospike.client.async.{ AsyncClient, AsyncClientPolicy }
import com.aerospike.client.Host
import com.aerospike.client.policy._
import com.aerospike.client.query.{ Statement, Filter }
import scala.collection.JavaConverters._
import eu.unicredit.reactive_aerospike.listener._
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.future.{ Factory, ScalaFactory }
import scala.annotation.tailrec
import java.util.HashSet
import com.aerospike.client.async.MaxCommandAction
import com.aerospike.client.AerospikeException
import eu.unicredit.reactive_aerospike.future.{ Future => ReactiveFuture }

object AerospikeClient {

  def apply(hostname: String, port: Int) =
    new AerospikeClient(hostname, port)(ScalaFactory)

}

class AerospikeClient[Future[_]](hosts: Host*)(implicit policy: AsyncClientPolicy = new AsyncClientPolicy(),
  factory: Factory[Future])
    extends AsyncClient(policy, hosts: _*) {

  def getFactory = factory

  def this(hostname: String, port: Int)(implicit factory: Factory[Future]) =
    this(new Host(hostname, port))
  def this(hostname: String, port: Int,
    policy: AsyncClientPolicy)(implicit factory: Factory[Future]) =
    this(new Host(hostname, port))(policy = policy, factory = factory)

  def checkConnection =
    if (!cluster.isConnected()) throw new AerospikeException("AerospikeClient not connected to cluster")

  def inner_put[K](key: AerospikeKey[K],
    bins: Seq[AerospikeBin[_]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): ReactiveFuture[AerospikeKey[K]] = {
    checkConnection
    implicit val converter = key.converter
    val wl = AerospikeWriteListener()
    super.put(wpolicy, wl, key.inner, bins.map(_.inner): _*)
    wl.result.map(_.key)
  }

  def put[K](key: AerospikeKey[K],
    bins: Seq[AerospikeBin[_]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
    factory.toBase(inner_put(key, bins))
  }

  def inner_append[K](key: AerospikeKey[K],
    bins: Seq[AerospikeBin[_]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): ReactiveFuture[AerospikeKey[K]] = {
    checkConnection
    implicit val converter = key.converter
    val wl = AerospikeWriteListener()
    super.append(wpolicy, wl, key.inner, bins.map(_.inner): _*)
    wl.result.map(_.key)
  }
  def append[K](key: AerospikeKey[K],
    bins: Seq[AerospikeBin[_]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
    factory.toBase(inner_append(key, bins))
  }

  def inner_prepend[K](key: AerospikeKey[K],
    bins: Seq[AerospikeBin[_]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): ReactiveFuture[AerospikeKey[K]] = {
    checkConnection
    implicit val converter = key.converter
    val wl = AerospikeWriteListener()
    super.prepend(wpolicy, wl, key.inner, bins.map(_.inner): _*)
    wl.result.map(_.key)
  }
  def prepend[K](key: AerospikeKey[K],
    bins: Seq[AerospikeBin[_]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
    factory.toBase(inner_prepend(key, bins))
  }

  def inner_delete[K](key: AerospikeKey[K])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): ReactiveFuture[Tuple2[AerospikeKey[K], Boolean]] = {
    checkConnection
    implicit val converter = key.converter
    val dl = AerospikeDeleteListener()
    super.delete(wpolicy, dl, key.inner)
    dl.result.map(_.key_existed)
  }
  def delete[K](key: AerospikeKey[K])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[Tuple2[AerospikeKey[K], Boolean]] = {
    factory.toBase(inner_delete(key))
  }

  def inner_touch[K](key: AerospikeKey[K])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): ReactiveFuture[AerospikeKey[K]] = {
    checkConnection
    implicit val converter = key.converter
    val wl = AerospikeWriteListener()
    super.touch(wpolicy, wl, key.inner)
    wl.result.map(_.key)
  }
  def touch[K](key: AerospikeKey[K])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
    factory.toBase(inner_touch(key))
  }

  def inner_exists[K](key: AerospikeKey[K])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): ReactiveFuture[Tuple2[AerospikeKey[K], Boolean]] = {
    checkConnection
    implicit val converter = key.converter
    val el = AerospikeExistsListener()
    super.exists(wpolicy, el, key.inner)
    el.result.map(_.key_existed)
  }
  def exists[K](key: AerospikeKey[K])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[Tuple2[AerospikeKey[K], Boolean]] = {
    factory.toBase(inner_exists(key))
  }

  /*
   * Fixed type to long
   */
  def inner_add[K](key: AerospikeKey[K],
    bins: Seq[AerospikeBin[Long]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): ReactiveFuture[AerospikeKey[K]] = {
    checkConnection
    implicit val converter = key.converter
    val wl = AerospikeWriteListener()
    super.add(wpolicy, wl, key.inner, bins.map(_.inner): _*)
    wl.result.map(_.key)
  }
  def add[K](key: AerospikeKey[K],
    bins: Seq[AerospikeBin[Long]])(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
    factory.toBase(inner_add(key, bins))
  }

  def inner_get(key: AerospikeKey[_],
    recordReader: AerospikeRecordReader)(implicit rpolicy: Policy = policy.readPolicyDefault): ReactiveFuture[(AerospikeKey[_], AerospikeRecord)] = {
    checkConnection
    implicit val keyConverter = key.converter
    val rl = AerospikeReadListener(recordReader)
    super.get(rpolicy, rl, key.inner)
    rl.result.map(x => x.key_record)
  }
  def get(key: AerospikeKey[_],
    recordReader: AerospikeRecordReader)(implicit rpolicy: Policy = policy.readPolicyDefault): Future[(AerospikeKey[_], AerospikeRecord)] = {
    factory.toBase(inner_get(key, recordReader))
  }

  def inner_getBins(key: AerospikeKey[_],
    binNames: Seq[String],
    recordReader: AerospikeRecordReader)(implicit rpolicy: Policy = policy.readPolicyDefault): ReactiveFuture[(AerospikeKey[_], AerospikeRecord)] = {
    checkConnection
    implicit val keyConverter = key.converter
    val rl = AerospikeReadListener(recordReader)
    super.get(rpolicy, rl, key.inner, binNames: _*)
    rl.result.map(x => x.key_record)
  }
  def getBins(key: AerospikeKey[_],
    binNames: Seq[String],
    recordReader: AerospikeRecordReader)(implicit rpolicy: Policy = policy.readPolicyDefault): Future[(AerospikeKey[_], AerospikeRecord)] = {
    factory.toBase(inner_getBins(key, binNames, recordReader))
  }

  def inner_getHeader(key: AerospikeKey[_],
    recordReader: AerospikeRecordReader)(implicit rpolicy: Policy = policy.readPolicyDefault): ReactiveFuture[(AerospikeKey[_], AerospikeRecord)] = {
    checkConnection
    implicit val keyConverter = key.converter
    val rl = AerospikeReadListener(recordReader)
    super.getHeader(rpolicy, rl, key.inner)
    rl.result.map(x => x.key_record)
  }
  def getHeader(key: AerospikeKey[_],
    recordReader: AerospikeRecordReader)(implicit rpolicy: Policy = policy.readPolicyDefault): Future[(AerospikeKey[_], AerospikeRecord)] = {
    factory.toBase(inner_getHeader(key, recordReader))
  }

  /*
    * homologous records
    */
  def inner_getMulti[T](keys: Seq[AerospikeKey[T]],
    recordReader: AerospikeRecordReader)(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): ReactiveFuture[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    checkConnection
    implicit val keyConverter = keys(0).converter
    val rl = AerospikeMultipleReadListener(recordReader)
    super.get(bpolicy, rl, keys.map(_.inner).toArray)
    rl.result.map(x => x.key_records)
  }
  def getMulti[T](keys: Seq[AerospikeKey[T]],
    recordReader: AerospikeRecordReader)(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    factory.toBase(inner_getMulti(keys, recordReader))
  }

  /*
    * NON homologous records
    */
  def inner_getMultiDifferent(keys_record_readers: Seq[(AerospikeKey[_], AerospikeRecordReader)])(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): ReactiveFuture[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    checkConnection
    val rl = AerospikeMultipleDifferentReadListener(keys_record_readers.map(x => (x._1.converter, x._2)))
    super.get(bpolicy, rl, keys_record_readers.map(_._1.inner).toArray)
    rl.result.map(x => x.key_records)
  }
  def getMultiDifferent(keys_record_readers: Seq[(AerospikeKey[_], AerospikeRecordReader)])(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    factory.toBase(inner_getMultiDifferent(keys_record_readers))
  }

  /*
    * homologous records selected bins
    */
  def inner_getMultiBins[T](keys: Seq[AerospikeKey[T]],
    binNames: Seq[String],
    recordReader: AerospikeRecordReader)(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): ReactiveFuture[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    checkConnection
    implicit val keyConverter = keys(0).converter
    val rl = AerospikeMultipleReadListener(recordReader)
    super.get(bpolicy, rl, keys.map(_.inner).toArray, binNames: _*)
    rl.result.map(x => x.key_records)
  }
  def getMultiBins[T](keys: Seq[AerospikeKey[T]],
    binNames: Seq[String],
    recordReader: AerospikeRecordReader)(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    factory.toBase(inner_getMultiBins(keys, binNames, recordReader))
  }

  /*
   * homologous record headers
   */
  def inner_getMultiHeader[T](keys: Seq[AerospikeKey[T]],
    recordReader: AerospikeRecordReader)(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): ReactiveFuture[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    checkConnection
    implicit val keyConverter = keys(0).converter
    val rl = AerospikeMultipleReadListener(recordReader)
    super.getHeader(bpolicy, rl, keys.map(_.inner).toArray)
    rl.result.map(x => x.key_records)
  }
  def getMultiHeader[T](keys: Seq[AerospikeKey[T]],
    recordReader: AerospikeRecordReader)(implicit bpolicy: BatchPolicy = policy.asyncBatchPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    factory.toBase(inner_getMultiHeader(keys, recordReader))
  }

  def inner_queryEqual[T](key_stub: AerospikeKey[T],
    recordReader: AerospikeRecordReader,
    filter: AerospikeBin[_])(implicit qpolicy: QueryPolicy = policy.queryPolicyDefault): ReactiveFuture[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    checkConnection
    val statement = new Statement()
    statement.setNamespace(key_stub.namespace)
    if (key_stub.setName.isDefined) {
      statement.setSetName(key_stub.setName.get)
    }

    statement.setFilters(Filter.equal(filter.name, filter.value.inner.toString))

    implicit val keyConverter = key_stub.converter
    val sl = AerospikeSequenceReadListener[T, Future](recordReader)
    super.query(qpolicy, sl, statement)
    sl.result.map(x => x.key_records)
  }
  def queryEqual[T](key_stub: AerospikeKey[T],
    recordReader: AerospikeRecordReader,
    filter: AerospikeBin[_])(implicit qpolicy: QueryPolicy = policy.queryPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    factory.toBase(inner_queryEqual(key_stub, recordReader, filter))
  }

  def inner_queryRange[T](key_stub: AerospikeKey[T],
    recordReader: AerospikeRecordReader,
    filterBinName: String,
    rangeMin: Long,
    rangeMax: Long)(implicit qpolicy: QueryPolicy = policy.queryPolicyDefault): ReactiveFuture[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    checkConnection
    val statement = new Statement()
    statement.setNamespace(key_stub.namespace)
    if (key_stub.setName.isDefined) {
      statement.setSetName(key_stub.setName.get)
    }

    statement.setFilters(
      Filter.range(filterBinName,
        rangeMin,
        rangeMax))

    implicit val keyConverter = key_stub.converter
    val sl = AerospikeSequenceReadListener[T, Future](recordReader)
    super.query(qpolicy, sl, statement)
    sl.result.map(x => x.key_records)
  }
  def queryRange[T](key_stub: AerospikeKey[T],
    recordReader: AerospikeRecordReader,
    filterBinName: String,
    rangeMin: Long,
    rangeMax: Long)(implicit qpolicy: QueryPolicy = policy.queryPolicyDefault): Future[Seq[(AerospikeKey[_], AerospikeRecord)]] = {
    factory.toBase(inner_queryRange(key_stub, recordReader, filterBinName, rangeMin, rangeMax))
  }

}
