package eu.unicredit.reactive_aerospike.test

import eu.unicredit.reactive_aerospike.client._
import eu.unicredit.reactive_aerospike.data._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import eu.unicredit.reactive_aerospike.data.AerospikeValue._

object NewTest extends App {
  println("Flattened everything a bit")  
  
  //val client = new AerospikeClient("localhost", 3000)
  val client = new AerospikeClient("localhost", 3000, 
      eu.unicredit.reactive_aerospike.future.ScalaFactory)
  
  val key = AerospikeKey("test", "demoRecord2", "123456")
  
  val key2 = AerospikeKey("test", "demoRecord2", "1234567")
  
  val bin1 = AerospikeBin("uno", 1.9)
  val bin12 = AerospikeBin("uno", 20D)
  val bin2 = AerospikeBin("due", "due")
  
  //to add a bit of syntactic sugar...
  val lista = AerospikeList(1.2,2.5,3.6)
  
  val intList1 = AerospikeList(1,2,3)
  val intList2 = AerospikeList(4,5,6)
  
  val lista2 = AerospikeList(intList1,intList2)
  
  //implicit val doubleListReader = listReader[Double]
  
  val bin3 = AerospikeBin("tre", lista)
  	  			//lista, listReader[Double])
  
  val bin4 = AerospikeBin("quattro", lista2)
  
  val mappa = AerospikeMap(
		  ("uno" -> 1),
		  ("due" -> 2)
      )
     
  val bin5 = AerospikeBin("cinque", mappa)//, mapReader[String,Int])
  
  val bins = Seq(
		  		bin1,
		  		bin2,
		  		bin3,
		  		bin4,
		  		bin5
		  	 )
  
		  	 
  val bins2 = Seq(
		  		bin12,
		  		bin2,
		  		bin3,
		  		bin4,
		  		bin5
		  	 )
  		  	 
  //val reader = AerospikeRecordReader(bins)
  val reader = new AerospikeRecordReader(
      Map(("uno" -> AerospikeDoubleReader),
          //("due" -> AerospikeStringReader),
          ("tre" -> listReader[Double]),
          ("quattro" -> listReader(AerospikeListReader[Int]())),
          ("cinque" -> mapReader[String,Int])
          )
      )
  
  /*
  class ModelObj[+T](key: AerospikeKey[T]) {
    
    val singleton: Dao[_]
  }
  
  class Dao[T] {
    
    
    def get[T] = {}
    def put(t: T) = {}
  }
  
  case class Qualcosa(
      key: String, 
      uno: Int,
      due: String,
      tre: List[String]
      ) extends ModelObj(AerospikeKey(key)) {
    
    val singleton = Qualcosa
  }
  
  object Qualcosa extends Dao[Qualcosa] {
    val namespace = "test"
      
    val set = "mySet"
    
   def writer(q: Qualcosa): Seq[Bin] = {
      "key" -> q.uno
    }   
      
   implicit val reader = new AerospikeRecordReader(
      Map(("key" -> AerospikeDoubleReader),
          //("due" -> AerospikeStringReader),
          ("tre" -> listReader[Double]),
          ("quattro" -> listReader(AerospikeListReader[Int]())),
          ("cinque" -> mapReader[String,Int])
          )
      )
  }
  
  Qualcosa.put(Qualcosa("key",1,"due",List("1","2")))
  */
  /*
  client.delete(null, key.inner)
  Thread.sleep(5000)
  System.exit(0)
  */
  import eu.unicredit.reactive_aerospike.future.ScalaFactory.Helpers._
  import scala.concurrent._
  //import eu.unicredit.reactive_aerospike.future.TwitterFactory.Helpers._
  //import com.twitter.util._
  
  val putted = client.put(key, bins)
  val putted2 = client.put(key2, bins2)

  for {
    puttedKey <- putted
    puttedKey2 <- putted2
  } yield {
    /*for {
      puttedKey2 <- putted2
    } yield*/ {
    
    
    println(s"element putted $puttedKey")
    
    val result = client.get(puttedKey, reader)

    client.get(puttedKey, reader).onComplete{
      case Success(getted) =>
         val binsR = getted._2
        
         binsR.getBins.foreach(bin =>
          println(s" ${bin.name} ${bin.value.getClass()} ${bin.value} ")
         )
       	println("OK")
        client.close()
      case Failure(err) =>
      	println("ERROR")
      	err.printStackTrace()
        client.close()
    }
    }
    /*
    client.getMulti(Seq(puttedKey,puttedKey2), reader).onComplete{
      case Success(getted) =>
         println(s"Getted records are ${getted.size}")
        
         getted.foreach{x => 
         val binsR = x._2
         
         binsR.getBins.foreach(bin =>
          println(s"Record ${x._1.userKey} ${bin.name} ${bin.value.getClass()} ${bin.value} ")
         )
         }
       	println("OK")
        client.close()
      case Failure(err) =>
      	println("ERROR")
      	err.printStackTrace()
        client.close()
    }
    */
    /*
    val g = client.get(key, reader)
    g.onSuccess{ getted =>
         val binsR = getted._2
        
         binsR.getBins.foreach(bin =>
          println(s" ${bin.name} ${bin.value.getClass()} ${bin.value} ")
         )
       	println("OK")
        client.close()
    }
    g.onFailure{err =>
      	println("ERROR")
      	err.printStackTrace()
        client.close()
    }
	*/
  }

  while(client.isConnected()) Thread.sleep(500)
  //while(!client.isConnected()) Thread.sleep(500)
  println("End")
  //System.exit(0)
}