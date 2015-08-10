package io.tabmo.aerospike.client.operations

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

import com.aerospike.client.Value
import com.aerospike.client.policy.QueryPolicy
import com.aerospike.client.query.{Filter, Statement}

import io.tabmo.aerospike.TSafe.VRestriction
import io.tabmo.aerospike.client.ReactiveAerospikeClient
import io.tabmo.aerospike.data.{AerospikeKey, AerospikeKeyConverter, AerospikeRecord}
import io.tabmo.aerospike.listener.AerospikeReadSequenceListener

trait QueryOperations {
  self: ReactiveAerospikeClient =>

  def queryEqual[K, V: VRestriction](
    namespace: String,
    set: String,
    bins: Seq[String],
    filterBinName: String,
    filterValue: V,
    policy: Option[QueryPolicy] = None)
      (implicit keyConverter: AerospikeKeyConverter[K]): Future[Map[AerospikeKey[K], AerospikeRecord]] = {

    logger.debug(s"QUERY EQUAL $namespace:$set ON $filterBinName = $filterValue (${bins.mkString(", ")}")

    val statement = new Statement()
    statement.setNamespace(namespace)
    statement.setSetName(set)
    statement.setBinNames(bins: _*)

    statement.setFilters(
      makeEqualFilter(filterBinName, filterValue)
    )

    val listener = AerospikeReadSequenceListener[K]()(keyConverter)
    asyncClient.query(policy.orNull, listener, statement)
    listener.result
  }

  def queryRange[K](
    namespace: String,
    set: String,
    bins: Seq[String],
    filterBinName: String,
    rangeMin: Long,
    rangeMax: Long,
    policy: Option[QueryPolicy] = None)
      (implicit keyConverter: AerospikeKeyConverter[K]): Future[Map[AerospikeKey[K], AerospikeRecord]] = {

    logger.debug(s"QUERY RANGE $namespace:$set ON $filterBinName BETWEEN $rangeMin and $rangeMax (${bins.mkString(", ")}")

    val statement = new Statement()
    statement.setNamespace(namespace)
    statement.setSetName(set)
    statement.setBinNames(bins: _*)

    statement.setFilters(
      Filter.range(filterBinName, rangeMin, rangeMax)
    )

    val listener = AerospikeReadSequenceListener[K]()(keyConverter)
    asyncClient.query(policy.orNull, listener, statement)
    listener.result
  }

  def queryEqualAggregate[V: VRestriction](
    namespace: String,
    set: String,
    filterBinName: String,
    filterValue: V,
    classloader: ClassLoader,
    resourcePath: String,
    packageName: String,
    functionName: String,
    args: Seq[Value] = Seq.empty,
    policy: Option[QueryPolicy] = None)
      (implicit ec: ExecutionContext): Future[Seq[AerospikeRecord]] = {

    logger.debug(s"QUERY EQUAL AGGREGATE $namespace:$set ON $filterBinName = $filterValue with $packageName.$functionName(${args.mkString(", ")})")

    val statement = new Statement()
    statement.setNamespace(namespace)
    statement.setSetName(set)
    statement.setAggregateFunction(classloader, resourcePath, packageName, functionName, args: _*)

    statement.setFilters(
      makeEqualFilter(filterBinName, filterValue)
    )

    Future {
      val result = asyncClient.queryAggregate(policy.orNull, statement)
      result.iterator().asScala.toList.map {
        case r: java.util.HashMap[_, _] =>
          val map = r.asScala.toMap.asInstanceOf[Map[String, AnyRef]]
          new AerospikeRecord(map, -1, -1)
        case r => throw new IllegalArgumentException(s"query result is of type ${r.getClass}, expecting HashMap")
      }
    }
  }

  private def makeEqualFilter[V: VRestriction](filterBinName: String, filterValue: V): Filter = {
    filterValue match {
      case i: Int => Filter.equal(filterBinName, i.toLong)
      case l: Long => Filter.equal(filterBinName, l)
      case s: String => Filter.equal(filterBinName, s)
    }
  }

}
