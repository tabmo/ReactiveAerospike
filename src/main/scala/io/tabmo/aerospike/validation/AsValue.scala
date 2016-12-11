package jto.validation
package aerospike

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import io.tabmo.aerospike.data.AerospikeRecord

sealed trait AsValue

object AsValue {

  private def apply(value: AnyRef): AsValue = {
    if (value == null) AsNull
    else {
      value match {
        case _: java.util.Map[_, _] => AsObject(value.asInstanceOf[java.util.Map[String, AnyRef]].asScala.mapValues(AsValue.apply).toMap)
        case _: java.util.List[_] => AsArray(value.asInstanceOf[java.util.List[AnyRef]].map(AsValue.apply).toIndexedSeq)
        case l: java.lang.Long => AsLong(l)
        case s: String => AsString(s)
        case _ => throw new IllegalStateException()
      }
    }
  }

  def apply(record: AerospikeRecord): AsValue = {
    this.apply(mapAsJavaMap(record.bins))
  }

  def obj(fields: (String, AsValue)*): AsObject = AsObject(fields.toMap)
  def arr(fields: AsValue*): AsArray = AsArray(fields.toIndexedSeq)
  def long(v: Long): AsLong = AsLong(v)
  def string(v: String): AsString = AsString(v)
}

case class AsObject(kv: Map[String, AsValue]) extends AsValue

case class AsArray(array: IndexedSeq[AsValue]) extends AsValue

case class AsLong(v: Long) extends AsValue

case class AsString(s: String) extends AsValue

case object AsNull extends AsValue