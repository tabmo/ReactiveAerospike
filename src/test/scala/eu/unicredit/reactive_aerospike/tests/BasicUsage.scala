package eu.unicredit.reactive_aerospike.tests

import org.scalatest._

import eu.unicredit.reactive_aerospike.client._
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.data.AerospikeValue._
import eu.unicredit.reactive_aerospike.future.ScalaFactory.Helpers._
import scala.util.{Success, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class BasicUsage extends FlatSpec {

  val client = new AerospikeClient("localhost", 3000)
  
  val key = AerospikeKey("test", "demokey", "123")
  
  val uno = 1.9
  val due = 2014L
  val tre = "tre"
  
  "An Aerospike Client" should "save sequences of bins under a key" in {
      
      val bin1 = AerospikeBin("uno", uno)
      val bin2 = AerospikeBin("due", due)
      val bin3 = AerospikeBin("tre", tre)
      
      val putted = client.put(key, Seq(bin1,bin2,bin3))
      
      val res = Await.result(putted, 100 millis)
      
      assert{ key.namespace == res.namespace }
      assert{ key.setName  == res.setName  }
      assert{ key.userKey == res.userKey }
  } 
  
  it should "retrieve the record" in {
    val reader = AerospikeRecordReader(
      Map(("uno" -> AerospikeDoubleReader),
          ("due" -> AerospikeLongReader),
          ("tre" -> AerospikeStringReader)))
          
    val getted = client.get(key, reader)
    
    val res = Await.result(getted, 100 millis)
    
    assert{ key.namespace == res._1.namespace }
    assert{ key.setName  == res._1.setName  }
    assert{ key.userKey == res._1.userKey }
    assert{ uno == res._2.get[Double]("uno").get.base }
    assert{ due == res._2.get[Long]("due").get.base }
    assert{ tre == res._2.get[String]("tre").get.base }
  }
  
  it should "save and retrieve complex data" in {
      val intList1 = AerospikeList(1,2,3)
      val intList2 = AerospikeList(4,5,6)
      val aList = AerospikeList(intList1,intList2)
      
      val aMap = AerospikeMap(
		  ("uno" -> 1),
		  ("due" -> 2)
      )
      
      val putted = client.put(key, Seq(
          AerospikeBin("aList",aList), AerospikeBin("aMap", aMap)))
      
      val put = Await.result(putted, 100 millis)
      
      assert{ key.namespace == put.namespace }
      assert{ key.setName  == put.setName  }
      assert{ key.userKey == put.userKey }
      
      val reader = AerospikeRecordReader(
    	Map(("aList" -> listReader(AerospikeListReader[Int]())),
    		("aMap" -> mapReader[String,Int])) )
        
      val getted = client.get(key, reader)
    
      val get = Await.result(getted, 100 millis)
    
      assert{ key.namespace == get._1.namespace }
      assert{ key.setName  == get._1.setName  }
      assert{ key.userKey == get._1.userKey }
      assert{ 4 == get._2.get[List[AerospikeList[Int]]]("aList").get.base(1).base(0).base }
      assert{ 2 == get._2.get[Map[AerospikeString, AerospikeInt]]("aMap").get.base.map(x => x._1.base -> x._2.base).get("due").get  }
  }

}