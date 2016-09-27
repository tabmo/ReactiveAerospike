package io.tabmo.aerospike.client.operations

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}

import com.aerospike.client.{AerospikeException, Key, Record, Value}
import com.aerospike.client.listener.RecordSequenceListener
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

    logger.timing(s"QUERY EQUAL $namespace:$set ON $filterBinName = $filterValue (bins: ${if (bins.isEmpty) "*" else bins.mkString(", ")})") {

      val statement = new Statement()
      statement.setNamespace(namespace)
      statement.setSetName(set)
      if (bins.nonEmpty) statement.setBinNames(bins: _*)

      statement.setFilters(
        makeEqualFilter(filterBinName, filterValue)
      )

      val listener = AerospikeReadSequenceListener[K]()(keyConverter)
      asyncClient.query(policy.orNull, listener, statement)

      listener.result
    }
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

    logger.timing(s"QUERY RANGE $namespace:$set ON $filterBinName BETWEEN $rangeMin and $rangeMax (bins: ${if (bins.isEmpty) "*" else bins.mkString(", ")})") {
      val statement = new Statement()
      statement.setNamespace(namespace)
      statement.setSetName(set)
      if (bins.nonEmpty) statement.setBinNames(bins: _*)

      statement.setFilters(
        Filter.range(filterBinName, rangeMin, rangeMax)
      )

      val listener = AerospikeReadSequenceListener[K]()(keyConverter)
      asyncClient.query(policy.orNull, listener, statement)
      listener.result
    }
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

    logger.timing(s"QUERY EQUAL AGGREGATE $namespace:$set ON $filterBinName = $filterValue with $packageName.$functionName(${args.mkString(", ")})") {
      queryAggregate(
        namespace = namespace,
        set = set,
        filter = makeEqualFilter(filterBinName, filterValue),
        classloader = classloader,
        resourcePath = resourcePath,
        packageName = packageName,
        functionName = functionName,
        args = args,
        policy = policy
      )
    }
  }

  def queryRangeAggregate(
   namespace: String,
   set: String,
   filterBinName: String,
   min: Long,
   max: Long,
   classloader: ClassLoader,
   resourcePath: String,
   packageName: String,
   functionName: String,
   args: Seq[Value] = Seq.empty,
   policy: Option[QueryPolicy] = None)(implicit ec: ExecutionContext): Future[Seq[AerospikeRecord]] = {

    logger.timing(s"QUERY RANGE AGGREGATE $namespace:$set ON $filterBinName between [$min,$max] with $packageName.$functionName(${args.mkString(", ")})") {
      queryAggregate(
        namespace = namespace,
        set = set,
        filter = Filter.range(filterBinName, min, max),
        classloader = classloader,
        resourcePath = resourcePath,
        packageName = packageName,
        functionName = functionName,
        args = args,
        policy = policy
      )
    }
  }

  private def queryAggregate(
   namespace: String,
   set: String,
   filter: Filter,
   classloader: ClassLoader,
   resourcePath: String,
   packageName: String,
   functionName: String,
   args: Seq[Value],
   policy: Option[QueryPolicy])(implicit ec: ExecutionContext): Future[Seq[AerospikeRecord]] = {
    val statement = new Statement()
    statement.setNamespace(namespace)
    statement.setSetName(set)
    statement.setAggregateFunction(classloader, resourcePath, packageName, functionName, args: _*)

    statement.setFilters(filter)

    val promise = Promise[Seq[AerospikeRecord]]()
    var results = List[AerospikeRecord]()
    asyncClient.query(null, new RecordSequenceListener {

      override def onRecord(key: Key, record: Record): Unit = {
        if(record.bins.containsKey("FAILURE")) promise.failure(new AerospikeException(-1,s"${record.bins.get("FAILURE")}"))
        else {
          record.bins match {
            case r: java.util.HashMap[_, _] => results = unwrapSuccess(r) :: results
            case r: Any => throw new IllegalArgumentException(s"query result is of type ${r.getClass}, expecting HashMap")
          }
        }
      }

      override def onFailure(exception: AerospikeException): Unit = {
        promise.failure(exception)
      }

      override def onSuccess(): Unit = {
        promise.success(results)
      }

    }, statement)

    promise.future
  }

  private def unwrapSuccess(record: java.util.HashMap[_, _]): AerospikeRecord = {
    record.get("SUCCESS") match {
      case r: java.util.HashMap[_, _] => new AerospikeRecord(r.asScala.toMap.asInstanceOf[Map[String, AnyRef]], -1, -1)
      case r: Any => throw new IllegalArgumentException(s"query result is of type ${r.getClass}, expecting HashMap")
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
