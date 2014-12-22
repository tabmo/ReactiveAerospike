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
import eu.unicredit.reactive_aerospike.future._
import eu.unicredit.reactive_aerospike.model._
import com.twitter.conversions.time._

class TwitterFutureUsage extends FlatSpec {
	import com.twitter.util._
	import TwitterFactory.Helpers._
  
	val client = new AerospikeClient("localhost", 3000, TwitterFactory)
  
	case class Person(id: String,
					  name: String,
					  surname: String,
					  age : Int) 
    	extends ModelObj[String](id) with EqualPerson {
  
		val dao = PersonDao
  
	}

	object PersonDao extends Dao[String, Person](client) {
  
		val namespace = "test"
  
		val setName = "people"
  
		val objWrite: Seq[AerospikeBinProto[Person,_]] =
			Seq(("name", (p: Person) => p.name),
				("surname", (p: Person) => p.surname),
				("age", (p: Person) => p.age))
  
		val objRead: (AerospikeKey[String],AerospikeRecord) => Person =
    		(key: AerospikeKey[String], record: AerospikeRecord) =>
    			Person(
    					key.userKey,
    					record.get[String]("name").get,
    					record.get[String]("surname").get,
    					record.get[Int]("age").get)
	}
  
	"An Aerospike Client" should "save and retrieve a person with Twitter Future and Promise" in {
	   val person1 = Person("tkey","Tizio", "tizio", 23)
	   val person2 = Person("ckey","Caio", "caio", 32)
	   
	   try {
		   Await.result(PersonDao.delete("tkey"),500.millis)
		   Await.result(PersonDao.delete("ckey"),500.millis)
	   } catch {
	     case _ : Throwable => 
	   }
	   
	   Await.result(PersonDao.create(person1),500.millis)
	   Await.result(PersonDao.create(person2),100.millis)
	   
	   val retP1 = Await.result(PersonDao.read("tkey"),100.millis)
	   val retP2 = Await.result(PersonDao.read("ckey"),100.millis)
	   
	   assert { person1 == retP1 }
	   assert { person2 == retP2 }
   } 
	
	
    trait EqualPerson {
	   self: Person =>
   		override def equals(p2: Any) = {
   				p2 match {
   					case per: Person =>
   					(
   							per.id == self.id &&
   							per.name == self.name &&
   							per.surname == self.surname &&
   							per.age == self.age
   					)
   					case _ => false
   				}
   			}
   	}
}

object TwitterFactory extends Factory {
	class TwitterFuture[+T]
		  (f: com.twitter.util.Future[T])
		  extends Future[T] {
    val inner = f
    
    def map[S](f: T => S)
    		  : Future[S] = {
    	val p = new TwitterPromise[S]
   		inner.onSuccess{value =>
   		  try
   		  	p.success(f(value))
   		  catch {
    		  	case err: Throwable =>
    		  		p.failure(err)
    	  }
   		}
    	inner.onFailure{err => p.failure(err)}
    	p.future
    }
  	def flatMap[S](f: T => Future[S])
  				: Future[S] = {
      val p = new TwitterPromise[S]
      inner.onSuccess{value => 
        try
        	f(value).map(x => {p.success(x)})
        catch {
        	case err: Throwable => 
        		p.failure(err)
        }
      }
      inner.onFailure{err => p.failure(err)}
      p.future
    }
  }
  
  class TwitterPromise[T] extends Promise[T] {
    val inner = com.twitter.util.Promise.apply[T]()
    def future: Future[T] = new TwitterFuture(inner.interruptible)
	def success(value: T): Unit = inner.setValue(value)
	def failure(exception: Throwable): Unit = inner.raise(exception)
  }

  
  def newPromise[T] = new TwitterPromise[T]
  
  object Helpers {
    
    implicit def fromTFToFuture[T](x: Future[T])
    			: com.twitter.util.Future[T] =
    			x match {
    				case sf: TwitterFuture[T] =>
    				  	sf.inner
    				case _ => throw new Exception("Wrong future type")
    			}
    implicit def fromFutureToSF[T](x: com.twitter.util.Future[T])
    			: TwitterFuture[T] =
    			new TwitterFuture(x) 
        
  }
}