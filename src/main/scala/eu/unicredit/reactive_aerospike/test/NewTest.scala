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
  
  val client = new AerospikeClient("localhost", 3000)
  
  val key = AerospikeKey("test", "demoRecord2", "123456")
  
  val bin1 = AerospikeBin("uno", 1.9)
  val bin2 = AerospikeBin("due", "due")
  
  //to add a bit of syntactic sugar...
  val lista = AerospikeList(1.2,2.5,3.6)
  
  //implicit val doubleListReader = listReader[Double]
  
  val bin3 = AerospikeBin("tre", lista)
  	  			//lista, listReader[Double])
  
  val bins = Seq(bin1,bin2, bin3)
  
  //val reader = AerospikeRecordReader(bins)
  val reader = new AerospikeRecordReader(
      Map(("uno" -> AerospikeDoubleReader),
          //("due" -> AerospikeStringReader),
          ("tre" -> listReader[Double])
          )
      )
  
  val putted = client.put(key, bins)
    
  for {
    puttedKey <- putted
  } yield {
    println(s"element putted $puttedKey")
    
    client.get(puttedKey, reader).onComplete{
      case Success(getted) =>
        
        val binsR = getted._2
        
        binsR.getBins.foreach(bin =>
          println(s" ${bin.name} ${bin.value.getClass()} ${bin.value} ")
          )
        
        
        println("OK")
      case Failure(err) => println("ERRORE!");err.printStackTrace()
    }

  }

  
  println("End")
}