package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.{Record, Value}
import scala.collection.JavaConverters._
import scala.collection.convert.WrapAsScala._
import AerospikeValue._
import AerospikeRecord._

AVANTI DA QUI DEVO DESERIALIZZARE I RECORD

Mappare Il record contro una Sequenza di Bin TIPATI

abstract class AerospikeRecord()
                      (implicit 
                          generation: AerospikeRecord.Generation,
                          expiration: AerospikeRecord.Expiration) {
  
  val bins: Seq[AerospikeBin[_]]
   
  def toRecordBins: Seq[(String, Object)] =
    bins.map(_.toRecordValue)
  
  val inner =
    new Record(
      toRecordBins.toMap.asJava,
      null,
      generation.i,
      expiration.i
    )
}

object AerospikeRecord {

  case class Generation(i: Int)
  case class Expiration(i: Int)

  object Defaults {
    //TDB correct defaults..
    implicit val generation = Generation(0)
    implicit val expiration = Expiration(0)
  }
  
  /*
  trait AerospikeRecordConverter[AR <: AerospikeRecord] {
    def read(bins: AerospikeBin[_]*): AerospikeRecord[AR]
      
  }
  
  implicit object GenericAerospikeRecordRW
  
  def apply[T <: AerospikeRecord](record: Record)
  	(implicit converter: AerospikeRecordConverter[T]): AerospikeRecord[T]= {
    AerospikeBin(bin.name, converter.fromValue(bin.value), converter)    
  }
  
  def apply[AR <: AerospikeRecord](record: Record)
  			(implicit generation: AerospikeRecord.Generation = Defaults.generation,
                      expiration: AerospikeRecord.Expiration = Defaults.expiration,
                      converter: AerospikeRecordConverter[AR])
  			: AR = {
    converter.read(record)
  }
  */
}