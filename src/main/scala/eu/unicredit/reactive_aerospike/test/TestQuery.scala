package eu.unicredit.reactive_aerospike.test

import eu.unicredit.reactive_aerospike.client._
import eu.unicredit.reactive_aerospike.model._
import eu.unicredit.reactive_aerospike.data._
import eu.unicredit.reactive_aerospike.data.AerospikeValue._

import scala.concurrent.ExecutionContext.Implicits.global
import eu.unicredit.reactive_aerospike.future.ScalaFactory.Helpers._

object TestQuery {
  
  val client = new AerospikeClient("localhost", 3000)

}