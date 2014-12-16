package eu.unicredit.reactive_aerospike.client

import com.aerospike.client.async.{ AsyncClient, AsyncClientPolicy }
import com.aerospike.client.Host
import com.aerospike.client.policy._

import scala.collection.JavaConverters._

import eu.unicredit.reactive_aerospike.listener._
import eu.unicredit.reactive_aerospike.data._

import eu.unicredit.reactive_aerospike.future._
import scala.concurrent.ExecutionContext

class AerospikeClient(hosts: Host*)
					 (implicit 
						policy: AsyncClientPolicy = new AsyncClientPolicy(),
						executionContext: ExecutionContext = ExecutionContext.Implicits.global,
						factory: Factory = ScalaFactory
					    ) 
					  extends AsyncClient(policy, hosts:_*){

  def this(hostname: String, port: Int) =
    this(new Host(hostname, port))
  def this(hostname: String, port: Int, 
		   policy: AsyncClientPolicy, 
		   executionContext: ExecutionContext) =
    this(new Host(hostname, port))(policy,executionContext)
  def this(hostname: String, port: Int, 
		   policy: AsyncClientPolicy) =
    this(new Host(hostname, port))(policy = policy)   
  def this(hostname: String, port: Int, 
		   executionContext: ExecutionContext) =
    this(new Host(hostname, port))(executionContext = executionContext)
  def this(hostname: String, port: Int, 
		   policy: AsyncClientPolicy, 
		   executionContext: ExecutionContext,
		   factory: Factory) =
    this(new Host(hostname, port))(policy,executionContext,factory)
  def this(hostname: String, port: Int, 
		   executionContext: ExecutionContext,
		   factory: Factory) =
    this(new Host(hostname, port))(executionContext = executionContext, factory = factory)   
    
    

    
	def put[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[_]])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
	  	implicit val converter = key.converter
	  	println("factory class is "+factory)
	  	val wl = AerospikeWriteListener()
		super.put(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}
   
	def append[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[_]])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
	  	implicit val converter = key.converter
	  	val wl = AerospikeWriteListener()
		super.append(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}
	
	def prepend[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[_]])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
	  	implicit val converter = key.converter
	  	val wl = AerospikeWriteListener()
		super.prepend(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}
	
  	def delete[K](key: AerospikeKey[K])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[Tuple2[AerospikeKey[K], Boolean]] = {
  	  	implicit val converter = key.converter
	  	val dl = AerospikeDeleteListener()
		super.delete(wpolicy, dl, key.inner)
		dl.result.map(_.key_existed)
	}
  	
  	def touch[K](key: AerospikeKey[K])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
  	  	implicit val converter = key.converter
	  	val wl = AerospikeWriteListener()
		super.touch(wpolicy, wl, key.inner)
		wl.result.map(_.key)
	}

  	def exists[K](key: AerospikeKey[K])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[Tuple2[AerospikeKey[K], Boolean]] = {
  	  	implicit val converter = key.converter
	  	val el = AerospikeExistsListener()
		super.exists(wpolicy, el, key.inner)
		el.result.map(_.key_existed)
	}
	/*
	 * Fixed type to long
	 */
  	def add[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[Long]])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
  	  	implicit val converter = key.converter
	  	val wl = AerospikeWriteListener()
		super.add(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}
  	
   	def get(key: AerospikeKey[_], recordReader: AerospikeRecordReader)
   			(implicit rpolicy: Policy = policy.readPolicyDefault): Future[(AerospikeKey[_], AerospikeRecord)] = {
	  	implicit val keyConverter = key.converter 
	  	val rl = AerospikeReadListener(recordReader)
	  	super.get(rpolicy,rl,key.inner)
	  	rl.result.map(x => x.key_record)
   }
}