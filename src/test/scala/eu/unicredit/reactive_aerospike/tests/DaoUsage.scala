package eu.unicredit.reactive_aerospike.tests

import org.scalatest._

import eu.unicredit.reactive_aerospike.client._
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.data.AerospikeValue._
import eu.unicredit.reactive_aerospike.future.ScalaFactory.Helpers._
import eu.unicredit.reactive_aerospike.model._
import scala.util.{Success, Failure}
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class DaoUsage extends FlatSpec {
	
	val client = new AerospikeClient("localhost", 3000)
  
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
  
  
   "An Aerospike Client" should "save and retrieve a person " in {
	   val person1 = Person("tkey","Tizio", "tizio", 23)
	   val person2 = Person("ckey","Caio", "caio", 32)
	   
	   try {
		   Await.result(PersonDao.delete("tkey"), 100 millis)
		   Await.result(PersonDao.delete("ckey"), 100 millis)
	   } catch {
	     case _ : Throwable => 
	   }
	   
	   Await.result(PersonDao.create(person1), 100 millis)
	   Await.result(PersonDao.create(person2), 100 millis)
	   
	   val retP1 = Await.result(PersonDao.read("tkey"), 100 millis)
	   val retP2 = Await.result(PersonDao.read("ckey"), 100 millis)
	   
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