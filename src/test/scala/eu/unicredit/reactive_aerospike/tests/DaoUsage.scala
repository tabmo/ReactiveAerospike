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
import eu.unicredit.reactive_aerospike.tests.model._
import eu.unicredit.reactive_aerospike.client.AerospikeClient

class DaoUsage extends FlatSpec {
  
  "An Aerospike Client" should "save and retrieve a person using Scala Futures " in {
	   import eu.unicredit.reactive_aerospike.future.ScalaFactory.Helpers._
	   import scala.concurrent.Await
	   import scala.concurrent.duration._
	   import scala.concurrent.ExecutionContext.Implicits.global
     
	   implicit val personDao = PersonDao(new AerospikeClient("localhost", 3000))
	   
	   val person1 = Person("tkey","Tizio", "tizio", 23)
	   val person2 = Person("ckey","Caio", "caio", 32)
	   
	   try {
		   Await.result(personDao.delete("tkey"), 100 millis)
		   Await.result(personDao.delete("ckey"), 100 millis)
	   } catch {
	     case _ : Throwable => 
	   }
	   
	   Await.result(personDao.create(person1), 100 millis)
	   Await.result(personDao.create(person2), 100 millis)
	   
	   val retP1 = Await.result(personDao.read("tkey"), 100 millis)
	   val retP2 = Await.result(personDao.read("ckey"), 100 millis)
	   
	   assert { person1 == retP1 }
	   assert { person2 == retP2 }

   }
   
   it should "save and retrieve a person with Twitter Futures" in {
	   import com.twitter.conversions.time._
	   import com.twitter.util._
	   import eu.unicredit.reactive_aerospike.tests.TwitterFactory.Helpers._
	   
	   implicit val personDao = PersonDao(new AerospikeClient("localhost", 3000, TwitterFactory))
	   
	   val person1 = Person("tkey","Tizio", "tizio", 23)
	   val person2 = Person("ckey","Caio", "caio", 32)
	   
	   try {
		   Await.result(personDao.delete("tkey"),500.millis)
		   Await.result(personDao.delete("ckey"),500.millis)
	   } catch {
	     case _ : Throwable => 
	   }
	   
	   Await.result(personDao.create(person1),500.millis)
	   Await.result(personDao.create(person2),100.millis)
	   
	   val retP1 = Await.result(personDao.read("tkey"),100.millis)
	   val retP2 = Await.result(personDao.read("ckey"),100.millis)
	   
	   assert { person1 == retP1 }
	   assert { person2 == retP2 }
   } 
    
   
   	
}