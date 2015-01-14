/* Copyright 2014 UniCredit S.p.A.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

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
      
      assert{ key == res}
  } 
  
  it should "retrieve the record" in {
    val reader = AerospikeRecordReader(
      Map(("uno" -> AerospikeDoubleReader),
          ("due" -> AerospikeLongReader),
          ("tre" -> AerospikeStringReader)))
          
    val getted = client.get(key, reader)
    
    val res = Await.result(getted, 100 millis)
    
    assert{ key == res._1 }
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
      
      assert{ key == put }
      
      val reader = AerospikeRecordReader(
    	Map(("aList" -> listReader(AerospikeListReader[Int]())),
    		("aMap" -> mapReader[String,Int])) )
        
      val getted = client.get(put, reader)
    
      val get = Await.result(getted, 100 millis)
    
      assert{ key == get._1 }
      assert{ 4 == get._2.get[List[AerospikeList[Int]]]("aList").get.base(1).base(0).base }
      assert{ 2 == get._2.get[Map[AerospikeString, AerospikeInt]]("aMap").get.base.map(x => x._1.base -> x._2.base).get("due").get  }
  }

}