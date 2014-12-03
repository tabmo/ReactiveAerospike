package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.{Record, Value}
import scala.collection.JavaConverters._
import scala.collection.convert.WrapAsScala._
import AerospikeValue._
import AerospikeRecord._

class AerospikeRecord(
						bins: Seq[AerospikeBin[_]],
						//converter : Seq[AerospikeValueConverter[_]]*/
						generation: AerospikeRecord.Generation,
                        expiration: AerospikeRecord.Expiration) {
  
  def getBins = bins
   
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
  

  
  case class SingleIntRecord(
						ib: AerospikeBin[Int]
						)(implicit 
                          generation: AerospikeRecord.Generation,
                          expiration: AerospikeRecord.Expiration) 
                          extends AerospikeRecord(Seq(ib),generation,expiration) {
    
    def prova = ib.value
  }
  
  implicit object SingleIntRecordReader 
  	extends AerospikeRecordConverter[SingleIntRecord] {
	  val generation = Defaults.generation
	  val expiration = Defaults.expiration 
	  val converters = Seq(AerospikeIntReader)
	  
	  def toRecord(ar: Seq[AerospikeBin[_]]): SingleIntRecord =
	    ar(0).value match {
      		case ib: AerospikeInt =>
      		  	SingleIntRecord(AerospikeBin(ar(0).name,ib.i ))(generation,expiration)
      		case _ => 
      		  throw new Exception("Cannot go from generic to instance")
	  	}
  } 
  
  trait AerospikeRecordConverter[+AR <: AerospikeRecord] {
    val generation: AerospikeRecord.Generation
    val expiration: AerospikeRecord.Expiration
    
    val converters: Seq[AerospikeValueConverter[_]]
    
    //maybe deeper
    @unchecked
    def toRecord(ar: Seq[AerospikeBin[_]]): AR
    //(new AerospikeRecord(bins, generation,expiration)).asInstanceOf[AR]
  }
  
  def apply[AR <: AerospikeRecord](record: Record)
  	(implicit converter: AerospikeRecordConverter[AR]): AR = {
  	  converter.toRecord(
   	      record.bins.
  	      zip(converter.converters).map(x =>
  	        AerospikeBin(x._1, x._2)
  	          ).toSeq
  	      )
  }
  
  /*
  trait AerospikeRecordConverter[AR <: AerospikeRecord] {
    def read(bins: AerospikeBin[_]*): AerospikeRecord[AR]
      
  }
  
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