package eu.unicredit.reactive_aerospike.test

import eu.unicredit.reactive_aerospike.client._
import eu.unicredit.reactive_aerospike.model._
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.data.AerospikeValue._
import scala.concurrent.ExecutionContext.Implicits.global
import eu.unicredit.reactive_aerospike.future._
import eu.unicredit.reactive_aerospike.future.ScalaFactory.Helpers._
import com.aerospike.client.policy.QueryPolicy
import com.aerospike.client.query.{Statement, Filter}

object TestQueryRAW extends App {
  println("Start")
  val client = new AerospikeClient("localhost", 3000)

  val key_stub = AerospikeKey("debugging", "people", "")
  
  val personReader = 
      AerospikeRecordReader(
      Map(("name" -> AerospikeStringReader),
          ("surname" -> AerospikeStringReader),
          ("age" -> AerospikeIntReader)
      ))
      
  val filterBinSurname = AerospikeBin("surname", "pippo")
  val filterBinAge = AerospikeBin("age", 32)
  
    for {
    	peoples <- client.queryEqual(key_stub, personReader, filterBinAge)
    } yield {
      println(s"\nQ1 FOUND:")
	  peoples.foreach(p => {
	  	val key = p._1
	  	val records = p._2
	  	println(s"Key: $key")
	  	records.getBins.foreach(bin =>
	  	  println(s"Bin: $bin")
	  	  )
	  })

	 for {
    	peoples2 <- client.queryEqual(key_stub, personReader, filterBinAge)
	 } yield {
      println(s"\nQ2 FOUND:")
	  peoples2.foreach(p => {
	  	val key = p._1
	  	val records = p._2
	  	println(s"Key: $key")
	  	records.getBins.foreach(bin =>
	  	  println(s"Bin: $bin")
	  	  )
	  })
	  
	  for {
    	peoples3 <- client.queryRange(key_stub, personReader, "age", 1, 20)
	 } yield {
      println(s"\nQ3 FOUND:")
	  peoples3.foreach(p => {
	  	val key = p._1
	  	val records = p._2
	  	println(s"Key: $key")
	  	records.getBins.foreach(bin =>
	  	  println(s"Bin: $bin")
	  	  )
	  })
	  for {
    	peoples4 <- client.queryRange(key_stub, personReader, "age", 29, 33)
	 } yield {
      println(s"\nQ4 FOUND:")
	  peoples4.foreach(p => {
	  	val key = p._1
	  	val records = p._2
	  	println(s"Key: $key")
	  	records.getBins.foreach(bin =>
	  	  println(s"Bin: $bin")
	  	  )
	  })
	  client.close
	 }
	 }
	  	
	 } 
    }

  while(client.isConnected()) Thread.sleep(200)
  println("End")
  
}