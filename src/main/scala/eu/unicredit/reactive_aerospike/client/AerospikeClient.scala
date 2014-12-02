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
	
	/**
	 * Asynchronously write record bin(s). 
	 * This method schedules the put command with a channel selector and returns.
	 * Another thread will process the command and send the results to the listener.
	 * <p>
	 * The policy specifies the transaction timeout, record expiration and how the transaction is
	 * handled when the record already exists.
	 * 
	 * @param policy				write configuration parameters, pass in null for defaults
	 * @param listener				where to send results, pass in null for fire and forget
	 * @param key					unique record identifier
	 * @param bins					array of bin name/value pairs
	 * @throws AerospikeException	if queue is full
	 */
	 /*
	public final void put(WritePolicy policy, WriteListener listener, Key key, Bin... bins) throws AerospikeException {		
		if (policy == null) {
			policy = writePolicyDefault;
		}
		AsyncWrite command = new AsyncWrite(cluster, policy, listener, key, bins, Operation.Type.WRITE);
		command.execute();
	}*/
    //Better to put directly a record?
    def put(key: AerospikeKey[_], bins: AerospikeBin[_]*)
			(implicit wpolicy: WritePolicy = policy.writePolicyDefault): Future[AerospikeKey[_]] = {
		val wl = AerospikeWriteListener()
		super.put(wpolicy, wl, key.inner, bins.map(_.inner):_*)
		wl.result.map(_.key)
	}

}