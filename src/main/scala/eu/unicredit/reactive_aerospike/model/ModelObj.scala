package eu.unicredit.reactive_aerospike.model

import eu.unicredit.reactive_aerospike.data.AerospikeValue

abstract class ModelObj[K](_key: AerospikeValue[K]) {
  type DAO = Dao[K, _]
  
  def getKey = _key 
    
  val dao: DAO
  
}