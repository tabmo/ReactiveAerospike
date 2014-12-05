package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.{Record, Value}
import scala.collection.JavaConverters._
import scala.collection.convert.WrapAsScala._
import AerospikeValue._

case class MyRecord(
		value1: AerospikeValue[Int],
		value2: AerospikeValue[String]
    ) extends AerospikeRecord {
  
  val dao = MyRecordDao
  
}

object MyRecordDao extends AerospikeRecordDao[MyRecord] {
   
 val converter = 
    Seq(
    	BinConv("value1", (record: MyRecord) => record.value1, AerospikeIntReader),
		BinConv("value2", (record: MyRecord) => record.value2, AerospikeStringReader)
	)
  	
  def fromRecord(implicit raw: Map[String, Object]): MyRecord = {
   MyRecord(
       doField("value1"),
       doField("value2")
   )
 }
     
}

case class BinConv[T <: AerospikeRecord,X]
		(name: String, valueOf: (T) => AerospikeValue[X], converter: AerospikeValueConverter[X])

abstract class AerospikeRecord {
	self =>  
  val dao: AerospikeRecordDao[_]
      
  val bins =
  	dao.converter.toRecord(this)
  
  val inner =
    new Record(
      bins.toMap.asJava,
      null,
      0,	//To Be Done
      0		//To Be Done
    )
    
}

trait AerospikeRecordDao[T <: AerospikeRecord] {
  
  implicit val converter: AerospikeConverter[T]
  
  
 /* 
  * def fromRecord(implicit raw: Map[String, Object]): T
  def doField(key: String)(implicit raw: Map[String, Object]): AerospikeValue[_] =
    AerospikeValue(raw(key), converter.find(bc => bc.name == key).get.converter)
    	
  def toRecord(record: AerospikeRecord): Seq[(String, Object)] = {
      converter.map(bc => {
        (bc.name, bc.valueOf(record.asInstanceOf[T]))
      })
  }
*/
}

trait AerospikeConverter[T <: AerospikeRecord] {
 val converter: Seq[BinConv[T,_]]
  /*  Seq(
    	BinConv("value1", (record: MyRecord) => record.value1, AerospikeIntReader),
		BinConv("value2", (record: MyRecord) => record.value2, AerospikeStringReader)
	)*/
	
  def doField(key: String)(implicit raw: Map[String, Object]): AerospikeValue[_] =
    AerospikeValue(raw(key), converter.find(bc => bc.name == key).get.converter)

  def fromRecord(implicit raw: Map[String, Object]): T //= 
  /*{
   MyRecord(
       doField("value1"),
       doField("value2")
   )
 }*/
  	
   def toRecord(record: T/*AerospikeRecord*/): Seq[(String, Object)] = {
      converter.map(bc => {
        (bc.name, bc.valueOf(record.asInstanceOf[T]))
      })
  }
  
}

object AerospikeRecord {
  
 

  //def apply[T <: AerospikeRecord] =
    
  
}