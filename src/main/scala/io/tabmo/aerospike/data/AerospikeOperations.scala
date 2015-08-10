package io.tabmo.aerospike.data

import com.aerospike.client.Operation

import io.tabmo.aerospike.TSafe.VRestriction

trait AerospikeOperations {
  def toOperation: Operation
}

object AerospikeOperations {

  case class prepend(binName: String, value: String) extends AerospikeOperations {
    @inline def toOperation = Operation.prepend(Bin(binName, value))
  }

  case class append(binName: String, value: String) extends AerospikeOperations {
    @inline def toOperation = Operation.append(Bin(binName, value))
  }

  case class put[V: VRestriction](binName: String, value: V) extends AerospikeOperations {
    @inline def toOperation = Operation.put(Bin[V](binName, value))
  }

  case class add(binName: String, value: Long) extends AerospikeOperations {
    @inline def toOperation = Operation.add(Bin(binName, value))
  }

  case class get(binName: String) extends AerospikeOperations {
    @inline def toOperation = Operation.get(binName)
  }

  case object getAll extends AerospikeOperations {
    @inline def toOperation = Operation.get()
  }

  /*
  Not yet implemented
  case class getHeader() extends AerospikeOperations {
    @inline def toOperation = Operation.getHeader()
  }*/

  case object touch extends AerospikeOperations {
    @inline def toOperation = Operation.touch()
  }
}
