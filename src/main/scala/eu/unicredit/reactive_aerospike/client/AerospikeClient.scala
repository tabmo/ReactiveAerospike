package eu.unicredit.reactive_aerospike.client

import com.aerospike.client.async.{ AsyncClient, AsyncClientPolicy }
import com.aerospike.client.Host
import com.aerospike.client.policy._

import scala.collection.JavaConverters._

import eu.unicredit.reactive_aerospike.listener._
import eu.unicredit.reactive_aerospike.data._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class AerospikeClient
						(hosts: Host*)
						(implicit policy: AsyncClientPolicy = new AsyncClientPolicy()) 
						extends AsyncClient(policy, hosts:_*){

  def this(hostname: String, port: Int)
  		  (implicit policy: AsyncClientPolicy = new AsyncClientPolicy()) =
    this(new Host(hostname, port))(policy)
    
	def put[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[_]])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
	  	val wl = AerospikeWriteListener()(key.converter)
		super.put(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}
   
	def append[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[_]])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
	  	val wl = AerospikeWriteListener()(key.converter)
		super.append(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}
	
	def prepend[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[_]])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
	  	val wl = AerospikeWriteListener()(key.converter)
		super.prepend(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}
	
  	def delete[K](key: AerospikeKey[K])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[Tuple2[AerospikeKey[K], Boolean]] = {
	  	val dl = AerospikeDeleteListener()(key.converter)
		super.delete(wpolicy, dl, key.inner)
		dl.result.map(_.key_existed)
	}
  	
  	def touch[K](key: AerospikeKey[K])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
	  	val wl = AerospikeWriteListener()(key.converter)
		super.touch(wpolicy, wl, key.inner)
		wl.result.map(_.key)
	}

  	def exists[K](key: AerospikeKey[K])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[Tuple2[AerospikeKey[K], Boolean]] = {
	  	val el = AerospikeExistsListener()(key.converter)
		super.delete(wpolicy, el, key.inner)
		el.result.map(_.key_existed)
	}
	/*
	 * Fixed type to long
	 */
  	def add[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[Long]])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
	  	val wl = AerospikeWriteListener()(key.converter)
		super.add(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}
  	
   	def get(key: AerospikeKey[_], recordReader: AerospikeRecordReader)
   			(implicit rpolicy: Policy = policy.readPolicyDefault): Future[(AerospikeKey[_], AerospikeRecord)] = {
	  	implicit val keyConverter = key.converter 
	  	val rl = AerospikeReadListener(recordReader)
	  	super.get(rpolicy,rl,key.inner)
	  	rl.result.map(x => (x.key , x.record))
   }
}