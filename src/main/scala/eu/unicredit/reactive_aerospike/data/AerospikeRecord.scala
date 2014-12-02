package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.Record

//duplicates missing...
case class AerospikeRecord(
                          bins: Seq[AerospikeBin[_]],
                          duplicates: Seq[Seq[AerospikeBin[_]]] = Seq()
                          )
                          (implicit 
                          generation: AerospikeRecord.Generation,
                          expiration: AerospikeRecord.Expiration){

//  def this(bins: AerospikeBin[_]*)(implicit 
//                          generation: AerospikeRecord.Generation,
//                          expiration: AerospikeRecord.Expiration) =
//      this(bins.toSeq/*, Seq()*/)(generation,expiration)

  /*
  val inner =
    new Record(
      bins.map(_.toRecordValue).toMap,
      List(),
      generation,
      expiration
    )
*/
}

object AerospikeRecord {

  case class Generation(i: Int)
  case class Expiration(i: Int)

  object Defaults {
    //TDB
    implicit val generation = Generation(0)
    implicit val expiration = Expiration(0)

  }

}