package io.tabmo.aerospike

import scala.util.control.NonFatal

import org.scalatest._

import io.tabmo.aerospike.client._

class Factory extends FlatSpec {

  "An aerospike client" can "be created from one host" in {
    try {
      ReactiveAerospikeClient.connect("aerospiketestserver", 3000).close()
    } catch {
      case NonFatal(ex) => fail(ex)
    }
  }

  "An aerospike client" can "be created from multiple hosts" in {
    try {
      ReactiveAerospikeClient("aerospiketestserver:3000", "aerospiketestserver").close()
    } catch {
      case NonFatal(ex) => fail(ex)
    }
  }

  "Invalid hostname" must "be rejected" in {
    intercept[IllegalArgumentException] {
      ReactiveAerospikeClient("localhost:port").close()
    }
  }

}
