package io.tabmo.aerospike.client

import scala.util.Try

import com.aerospike.client._
import com.aerospike.client.async.{AsyncClient, AsyncClientPolicy}

import org.slf4j.LoggerFactory

import io.tabmo.aerospike.client.operations._

object ReactiveAerospikeClient {

  def connect(hostname: String, port: Int)(implicit policy: AsyncClientPolicy = new AsyncClientPolicy()): ReactiveAerospikeClient = {
    new ReactiveAerospikeClient(new AsyncClient(policy, hostname, port))
  }

  def apply(host: String, others: String*)(implicit policy: AsyncClientPolicy = new AsyncClientPolicy()): ReactiveAerospikeClient = {
    val hosts = (host +: others).map(parseHost)
    new ReactiveAerospikeClient(new AsyncClient(policy, hosts: _*))
  }

  private def parseHost(hostString: String): Host = {
    hostString.split(":") match {
      case Array(host) => new Host(host, 3000)
      case Array(host, port) if Try(port.toInt).isSuccess => new Host(host, port.toInt)
      case _ => throw new IllegalArgumentException(s"Invalid hostname provided ($hostString). It must be in the format `host` or `host:port`")
    }
  }
}

class ReactiveAerospikeClient(val asyncClient: AsyncClient)
  extends BasicOperations
  with IndexOperations
  with QueryOperations
  with UdfOperations
  with OperateOperations {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  def checkConnection() = {
    if (!asyncClient.isConnected) throw new AerospikeException("AerospikeClient not connected to cluster")
  }

  def close() = asyncClient.close()

}
