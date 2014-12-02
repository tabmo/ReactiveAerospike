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

	//-------------------------------------------------------
	// Write Record Operations
	//-------------------------------------------------------
	
    def put[T](key: AerospikeKey[T], bins: AerospikeBin[_]*)
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[T]] = {
	  	val wl = AerospikeWriteListener()(key.converter)
		super.put(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}
  
   def put[T](key: AerospikeKey[T], record: AerospikeRecord)
   			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[T]] = {
		val wl = AerospikeWriteListener()(key.converter)
		super.put(wpolicy, wl, key.inner, record.bins.map(_.inner):_*)
		wl.result.map(_.key)
  	}
  
   	//-------------------------------------------------------
	// Read Record Operations
	//-------------------------------------------------------
   
   /*
   public final void get(Policy policy, RecordListener listener, Key key) throws AerospikeException {
		if (policy == null) {
			policy = readPolicyDefault;
		}
		AsyncRead command = new AsyncRead(cluster, policy, listener, key, null);
		command.execute();
	}*/
   /*
   def get(key: AerospikeKey[_])
   			(implicit rpolicy: Policy = policy.readPolicyDefault): Future[AerospikeKey[_]] = {
     val rl = AerospikeReadListener()
     
   }
  */ 
}