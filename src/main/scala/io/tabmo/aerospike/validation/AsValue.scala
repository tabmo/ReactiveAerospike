package jto.validation
package aerospike

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import com.aerospike.client.Bin
import io.tabmo.aerospike.data.AerospikeRecord

sealed trait AsValue {
  def asObject = this.asInstanceOf[AsObject]
}

object AsValue {

  private def buildAST(value: AnyRef): AsValue = {
    if (value == null) AsNull
    else {
      value match {
        case _: java.util.Map[_, _] => AsObject(value.asInstanceOf[java.util.Map[String, AnyRef]].asScala.mapValues(buildAST).toMap)
        case _: java.util.List[_] => AsArray(value.asInstanceOf[java.util.List[AnyRef]].map(buildAST).toIndexedSeq)
        case l: java.lang.Long => AsLong(l)
        case s: String => AsString(s)
      }
    }
  }

  def unpackAST(value: AsValue): Object = value match {
    case AsObject(map) => map.mapValues(unpackAST).asJava
    case AsArray(seq) => seq.map(unpackAST).toList.asJava
    case AsLong(l) => new java.lang.Long(l)
    case AsString(s) => s
  }

  def apply(record: AerospikeRecord): AsValue = {
    buildAST(mapAsJavaMap(record.bins))
  }

  def obj(fields: (String, AsValue)*): AsObject = AsObject(fields.toMap)
  def arr(fields: AsValue*): AsArray = AsArray(fields.toIndexedSeq)
  def long(v: Long): AsLong = AsLong(v)
  def string(v: String): AsString = AsString(v)
}

case class AsObject(kv: Map[String, AsValue] = Map()) extends AsValue {
  def ++(obj: AsObject): AsObject = AsObject(kv ++ obj.kv)

  def toSeqBins: Seq[Bin] = kv.toList.map { case (k, v) => new Bin(k, AsValue.unpackAST(v)) }
}

object AsObject {
  def apply[V <: AsValue](key: String, value: V): AsObject = AsObject(Map(key -> value))
}

case class AsArray(array: IndexedSeq[AsValue]) extends AsValue

case class AsLong(v: Long) extends AsValue

case class AsString(s: String) extends AsValue

case object AsNull extends AsValue