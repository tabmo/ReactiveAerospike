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

    def put[K](key: AerospikeKey[K], bins: Seq[AerospikeBin[_]])
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[K]] = {
	  	val wl = AerospikeWriteListener()(key.converter)
		super.put(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}
   
   	//-------------------------------------------------------
	// Read Record Operations
	//-------------------------------------------------------
  def get(key: AerospikeKey[_], recordReader: AerospikeRecordReader)
   			(implicit rpolicy: Policy = policy.readPolicyDefault): Future[(AerospikeKey[_], AerospikeRecord)] = {
     implicit val keyConverter = key.converter 
     val rl = AerospikeReadListener(recordReader)
     super.get(rpolicy,rl,key.inner)
     rl.result.map(x => (x.key , x.record))
   }
}