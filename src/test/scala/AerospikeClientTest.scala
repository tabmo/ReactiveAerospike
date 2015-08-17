package io.tabmo.aerospike

import io.tabmo.aerospike.client.ReactiveAerospikeClient

trait AerospikeClientTest {

  protected val host = System.getProperty("aerospike.host", "aerospiketestserver")
  protected val port = System.getProperty("aerospike.port", "3000").toInt
  protected lazy val client = ReactiveAerospikeClient.connect(host, port)

}
