package eu.unicredit.reactive_aerospike.test

import eu.unicredit.reactive_aerospike.client._
import eu.unicredit.reactive_aerospike.model._
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.data.AerospikeValue._

import scala.concurrent.ExecutionContext.Implicits.global
import eu.unicredit.reactive_aerospike.future.ScalaFactory.Helpers._

object PersistanceTest extends App {
	val client = new AerospikeClient("localhost", 3000)
	
	val andrea = Person("andrea","andrea","peruffo",32)
	val pippo = Person("pippo","pippo","pippo",32)
	
	for {
		exa <- PersonDao.delete("andrea")
	} yield {
	  println("deleted old key andrea")
	}
	for {
		exa <- PersonDao.delete("pippo")
	} yield {
	  println("deleted old key pippo")
	}
	Thread.sleep(1000)
	
	for {
		ak <- PersonDao.create(andrea)
		pk <- PersonDao.create(pippo)
	} yield {
		
	  for {
	    checkAndrea <- PersonDao.read("andrea")
	  } {
		  println(s"ANDREA: Sono uguali? ${andrea==checkAndrea}")
	  }
	  for {
	    checkPippo <- PersonDao.read("pippo")
	  } {
		  println(s"PIPPO: Sono uguali? ${pippo==checkPippo}")
		  client.close
	  }
	}

	while(client.isConnected()) Thread.sleep(1000)
	println("End")
}

case class Person(
    id: String,
    name: String,
    surname: String,
    age : Int) 
    extends ModelObj[String](id) {
  
  val dao = PersonDao
  
  override def equals(p2: Any) = {
    p2 match {
      case per: Person =>
        (
        per.id == id &&
        per.name == name &&
        per.surname == surname &&
        per.age == age
        )
      case _ => false
    }
  }
}

object PersonDao extends Dao[String, Person] {
  
  val client = PersistanceTest.client
  
  val namespace = "debugging"
  
  val set = "people"
  
  val keyConverter = AerospikeStringReader
  
  val objWrite: Seq[AerospikeBinProto[Person,_]] =
      Seq(
    	("name", (p: Person) => p.name, AerospikeStringReader),
    	("surname", (p: Person) => p.surname, AerospikeStringReader),
    	("age", (p: Person) => p.age, AerospikeIntReader)    	
      )
  
  val objRead: (AerospikeKey[String],AerospikeRecord) => Person =
    {(key: AerospikeKey[String], record: AerospikeRecord) =>
      Person(
          key.userKey,
          record.get[String]("name").get,
          record.get[String]("surname").get,
          record.get[Int]("age").get
      )
    }
  
}