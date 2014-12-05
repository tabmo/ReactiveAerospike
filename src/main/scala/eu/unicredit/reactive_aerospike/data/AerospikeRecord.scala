//package eu.unicredit.reactive_aerospike.data
//
//import com.aerospike.client.{Record, Value}
//import scala.collection.JavaConverters._
//import scala.collection.convert.WrapAsScala._
//import AerospikeValue._
//import AerospikeRecord._
//
//object AerospikeRecord {
//
//  case class Generation(i: Int)
//  case class Expiration(i: Int)
//
//  object Defaults {
//    //TDB correct defaults..
//    implicit val generation = Generation(0)
//    implicit val expiration = Expiration(0)
//  }
//  
//
//  /*
//   * examples...
//   */
//  /*
//  case class SingleIntRecord(
//						ib: AerospikeValue[Int]
//						)(implicit 
//                          generation: AerospikeRecord.Generation,
//                          expiration: AerospikeRecord.Expiration) 
//                          extends AerospikeRecord(Seq(("ib" -> ib)),generation,expiration) {
//    
//    def prova = ib
//  }*/
//  
//  trait AerospikeRecordConverter[+AR <: AerospikeRecord[AR]] {
//    implicit val generation: AerospikeRecord.Generation = Defaults.generation
//    implicit val expiration: AerospikeRecord.Expiration = Defaults.expiration 
//    
//    val converter: Map[String, (AerospikeValueConverter[_], (AR) => AerospikeValue[_])]
//    //val converters: Map[String,AerospikeValueConverter[_]]
//    
//    def toRecord(ar: Map[String,Object]): AR =
//      	(AR.apply(converter.map(kv => kv._2._1.fromValue(Value.get(ar(kv._1))))))
//
//  }
//  
//  
//  case class SingleIntRecord(ib: AerospikeValue[Int])(implicit 
//                          generation: AerospikeRecord.Generation,
//                          expiration: AerospikeRecord.Expiration) 
//  			extends AerospikeRecord(generation,expiration,SingleIntRecordConverter) {
//    
//   }
//   
//  implicit object SingleIntRecordConverter 
//  			extends AerospikeRecordConverter[SingleIntRecord] {
//    
//    def convert: Map[String, (AerospikeValueConverter[_], (SingleIntRecord) => AerospikeValue[_])] =
//      Map(
//          "ib" -> (AerospikeIntReader -> ((x) => x.ib)) //vediamo se vale_.ib
//          )
//     
//  }
//  
//  /*
//  implicit object SingleIntRecordReader 
//  	extends AerospikeRecordConverter[SingleIntRecord] {
//	  val converters = Map("ib" -> AerospikeIntReader)
//	    
//	  def toRecord(ar: Map[String,AerospikeValue[_]]): SingleIntRecord =
//	    SingleIntRecord(ar("ib"))
//
//  }
//  
//  case class IntStringRecord(
//						ib: AerospikeValue[Int],
//						is: AerospikeValue[String]
//						)(implicit 
//                          generation: AerospikeRecord.Generation,
//                          expiration: AerospikeRecord.Expiration) 
//                          extends AerospikeRecord(Seq(("ib" -> ib),("is" -> is)),generation,expiration) {
//    
//    def prova = (ib, is)
//  }
//  
//  implicit object IntStringRecordReader 
//  	extends AerospikeRecordConverter[IntStringRecord] {
//	  val converters = Map(
//			  				"ib" -> AerospikeIntReader, 
//			  				"is" -> AerospikeStringReader)
//	    
//	  def toRecord(ar: Map[String,AerospikeValue[_]]): IntStringRecord =
//	    IntStringRecord(ar("ib"), ar("is"))
//
//  }
//  
//  case class IntStringLongRecord(
//						ib: AerospikeValue[Int],
//						is: AerospikeValue[String],
//						il: AerospikeValue[Long]
//						)(implicit 
//                          generation: AerospikeRecord.Generation,
//                          expiration: AerospikeRecord.Expiration) 
//                          extends AerospikeRecord(Seq(("ib" -> ib),("is" -> is),("il" -> il)),generation,expiration) {
//    
//    def prova = (ib, is, il)
//  }
//  
//  implicit object IntStringLongRecordReader 
//  	extends AerospikeRecordConverter[IntStringLongRecord] {
//	  val converters = 
//	    Map(
//	        "ib" -> AerospikeIntReader, 
//	        "is" -> AerospikeStringReader, 
//	        "il" -> AerospikeLongReader)
//	    
//	  def toRecord(ar: Map[String,AerospikeValue[_]]): IntStringLongRecord =
//	    IntStringLongRecord(ar("ib"), ar("is"), ar("il"))
//
//  }
//  */
//  /*
//   * examples ended ...
//   * 
//   */
//  
//  def apply[AR <: AerospikeRecord](record: Record)
//  	(implicit converter: AerospikeRecordConverter[AR]): AR = {
//  	  converter.toRecord(
//  		  record.bins.map(b => (b,converter.converters(b._1)))
//  		  .map(x => {
//  	        val bin = x._1
//  	        val conv = x._2
//  	        bin._1 -> conv.fromValue(Value.get(bin._2)) 
//  	      }).toMap
//  	      )
//  }
//  
//  /*
//  trait AerospikeRecordConverter[AR <: AerospikeRecord] {
//    def read(bins: AerospikeBin[_]*): AerospikeRecord[AR]
//      
//  }
//  
//  def apply[T <: AerospikeRecord](record: Record)
//  	(implicit converter: AerospikeRecordConverter[T]): AerospikeRecord[T]= {
//    AerospikeBin(bin.name, converter.fromValue(bin.value), converter)    
//  }
//  
//  def apply[AR <: AerospikeRecord](record: Record)
//  			(implicit generation: AerospikeRecord.Generation = Defaults.generation,
//                      expiration: AerospikeRecord.Expiration = Defaults.expiration,
//                      converter: AerospikeRecordConverter[AR])
//  			: AR = {
//    converter.read(record)
//  }
//  */
//}
//
//class AerospikeRecord[AR <: AerospikeRecord[_]](
//						//bins: Seq[AerospikeBin[_]],
//						generation: AerospikeRecord.Generation,
//                        expiration: AerospikeRecord.Expiration,
//                        converter: AerospikeRecordConverter[AR]
//                            ) {		  
//  def getBins =
//    converter.converter.map(x => AerospikeBin(x._1, x._2._2(this), x._2._1))
//   
//  def toRecordBins: Seq[(String, Object)] =
//    bins.map(_.toRecordValue)
//  
//  val inner =
//    new Record(
//      toRecordBins.toMap.asJava,
//      null,
//      generation.i,
//      expiration.i
//    )
//}
