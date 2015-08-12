package io.tabmo.aerospike

import com.aerospike.client.async.AsyncClientPolicy
import com.aerospike.client.policy.{QueryPolicy, WritePolicy, Policy}

import io.tabmo.aerospike.client.ReactiveAerospikeClient

trait AerospikeClientTest {

  private val policy = {
    val general = new AsyncClientPolicy()

    general.readPolicyDefault = {
      val p = new Policy()
      p.maxRetries = 5
      p.sleepBetweenRetries = 1000
      p
    }

    general.writePolicyDefault = {
      val p = new WritePolicy()
      p.maxRetries = 5
      p.sleepBetweenRetries = 1000
      p
    }

    general.queryPolicyDefault = {
      val p = new QueryPolicy()
      p.maxRetries = 5
      p.sleepBetweenRetries = 1000
      p
    }

    general
  }

  protected val client = ReactiveAerospikeClient.connect("aerospiketestserver", 3000)(policy)

}
