package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.Record
import scala.collection.JavaConverters._

case class AerospikeRecord(
                          bins: AerospikeBin[_]*//,
                          //duplicates: Seq[Seq[AerospikeBin[_]]] = Seq()
                          )
                          (implicit 
                          generation: AerospikeRecord.Generation,
                          expiration: AerospikeRecord.Expiration){
/*
  def this(bins: Seq[AerospikeBin[_]])(implicit 
                          generation: AerospikeRecord.Generation,
                          expiration: AerospikeRecord.Expiration) =
      this(bins/*, Seq()*/)(generation,expiration)
*/
  
  val inner =
    new Record(
      bins.map(_.toRecordValue).toMap.asJava,
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
  def apply(record: Record)
  			(implicit generation: AerospikeRecord.Generation = Defaults.generation,
                      expiration: AerospikeRecord.Expiration = Defaults.expiration)
  			: AerospikeRecord = {
	  AerospikeRecord(
	      record.bins.map(AerospikeBin(_):*)
	      )
  }
*/
}