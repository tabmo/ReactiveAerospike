package eu.unicredit.reactive_aerospike.test

import eu.unicredit.reactive_aerospike.client._
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.data.AerospikeRecord.Defaults._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import eu.unicredit.reactive_aerospike.data.AerospikeValue._
import eu.unicredit.reactive_aerospike.data.AerospikeRecord._

object Test extends App {

  println("Starting test...")

  /*
  val record =
    new AerospikeRecord(
      ("numero" -> 5),
      ("stringa" -> "pippo")
    )
    */
  
  val client = new AerospikeClient("localhost", 3000)
  
  val key = AerospikeKey("test", "demoRecord2", "12345")
  //val bin: AerospikeBin[Int] = ("prova" -> 1234)
  
  val record = SingleIntRecord(1234)
  
  
  val record2 =
    IntStringLongRecord(
        1234,
        "CIAO!",
        123456789L
        )
       
  /*
  bin.value match {
    case int: AerospikeInt =>
      println("E' un int!!! "+int)
    case _ => println("Errore!!!")
  }
 */

  //import com.aerospike.client._
  //import com.aerospike.client.async._
  /*
  		val policy = new AsyncClientPolicy();
		policy.user = "";
		policy.password = "";
		policy.asyncMaxCommands = 300;
		policy.asyncSelectorThreads = 1;
		policy.asyncSelectorTimeout = 10;
		policy.failIfNotConnected = true;
		policy.writePolicyDefault.timeout = 50
		policy.writePolicyDefault.sleepBetweenRetries = 1

  
  val client = new com.aerospike.client.async.AsyncClient("localhost", 3000)
*/		
  //client.delete(policy.writePolicyDefault, key.inner)
 /*
  while(!client.isConnected()) {
		println("waiting")
		Thread.sleep(1000);
		}
	*/	
 val end = Promise[Boolean]
	
  /*
  client.put(policy.writePolicyDefault, new com.aerospike.client.listener.WriteListener() {
			def onSuccess(key: Key) {
				println("Get: namespace=%s set=%s key=%s", key.namespace, key.setName, key.userKey)
				end.trySuccess(true)
			}
			def onFailure(e: AerospikeException) {
				println("Failed to put: namespace=%s set=%s key=%s exception=%s", key.namespace, key.setName, key.userKey, e.getMessage())
				end.trySuccess(false)
			}
		}, key.inner , bin.inner )
  */
  //client.put(null, key, bin)
  //val prova = client.get(null, key)

  client.delete(null, key.inner)
 Thread.sleep(2000)
   client.put(key, record2).onComplete{
      case Success(res) => 
        	println(s"GREIT $res")
        	
        	client.get[IntStringLongRecord](res).onComplete{
        	  case Success(ok) =>
        	    	println("Looks ok!")
        	    	println("Result is"+
        	    				ok._1.userKey+
        	    				" record is: "+
        	    				ok._2.getClass().getName()+
        	    				" ")
        	    	
        	    	ok._2 match {
        	    	  case sir: IntStringLongRecord =>
        	    	    println("OOOOK "+sir.prova)
        	    	  case any =>
        	    	    println("azz è un any")
        	    	}
        	    	
        	    	end.trySuccess(true)
        	  case Failure(err) =>
        	    	println("err2 "+err)
        	}
        	
      case Failure(err) => 
        	println("ERRORE!!!! "+err)
        	end.trySuccess(false)
  }

 /* this works
    println("--> ")
 client.delete(null, key.inner)
 Thread.sleep(2000)
 
  client.put(key, record).onComplete{
      case Success(res) => 
        	println(s"GREIT $res")
        	
        	client.get[SingleIntRecord](res).onComplete{
        	  case Success(ok) =>
        	    	println("Looks ok!")
        	    	println("Result is"+
        	    				ok._1.userKey+
        	    				" record is: "+
        	    				ok._2.getClass().getName()+
        	    				" ")
        	    	
        	    	ok._2 match {
        	    	  case sir: SingleIntRecord =>
        	    	    println("OOOOK "+sir.prova)
        	    	  case any =>
        	    	    println("azz è un any")
        	    	}
        	    	
        	    	end.trySuccess(true)
        	  case Failure(err) =>
        	    	println("err2 "+err)
        	    	end.trySuccess(false)
        	}
        	
      case Failure(err) => 
        	println(err)
        	end.trySuccess(false)
  }
  */ 
  /*
  for {
	  ret <- client.put(key, bin)
  } yield {
	 println("returned is "+ret)
     val record = client.get(null, key)

     println("--> ")
     println(record)

     client.close()
  }
  */
  for {
    readyToClose <- end.future 
  } yield {
    println("Closing client")
    Thread.sleep(3000)
    client.close()
  }
  
  val ended = Await.result(end.future, 5 seconds)
  
  println("Ended")
  System.exit(0)
}