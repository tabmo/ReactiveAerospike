package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.Value
import com.aerospike.client.Value._
import com.aerospike.client.util.Packer
import com.aerospike.client.lua.LuaInstance

trait AerospikeValue[+T <: Any] extends Value {
  val inner: Value

  override def estimateSize() =
    inner.estimateSize()

  override def write(buffer: Array[Byte], offset: Int) =
    inner.write(buffer, offset)

  override def pack(packer: Packer) =
    inner.pack(packer)

  override def getType() =
    inner.getType

  override def getObject() =
    inner.getObject

  //To be updated
  override def getLuaValue(instance: LuaInstance) =
    inner.getLuaValue(instance)

  override def toString() =
    inner.toString()
}

object AerospikeValue {

  def tryo[T](x: => T): Option[T] =
    try Some(x)
    catch {
      case err: Throwable => None
    }

  def apply(x: Any): AerospikeValue[_] = {
    if (x == null) AerospikeNull()
    else
      x match {
      	case av: AerospikeValue[_] => av
        case s: String => AerospikeString(s)
        case i: Int => AerospikeInteger(i)
        case l: Long => AerospikeInteger(l)
        case any =>
          	println(s"Match not found! ${any} ${any.getClass}")
          	AerospikeNull()
      }
  }

  case class AerospikeNull()
      extends AerospikeValue[Null] {
    override val inner = new NullValue
  }
  //  implicit def fromObjectToNull(o: java.lang.Object) =
  //    AerospikeNull

  case class AerospikeString(s: String)
      extends AerospikeValue[String] {
    override val inner = new StringValue(s)
  }

  //  implicit def fromObjectToString(o: java.lang.Object) =
  //    AerospikeString(o.asInstanceOf[String])

  case class AerospikeInteger(i: Long)
      extends AerospikeValue[Long] {
    override val inner = new LongValue(i)
  }

  //  implicit def fromObjectToInteger(o: java.lang.Object) =
  //    AerospikeInteger(o.asInstanceOf[Long])

  /*
  //da qui in avanti implementazioni parziali da finire

  case class AerospikeBlob(ba: Array[Byte])
      extends AerospikeValue[Array[Byte]] {
    def toNative = ba
  }

  implicit def fromObjectToBlob(o: java.lang.Object) =
    AerospikeBlob(o.asInstanceOf[Array[Byte]])

  case class AerospikeMap[T <: AerospikeValue[T]](m: Map[String, T])
      extends AerospikeValue[Map[String, T]] {
    def toNative = m.map(x => (x._1 -> x._2.toNative))
  }

  case class AerospikeList[T <: AerospikeValue[T]](l: List[T])
      extends AerospikeValue[List[T]] {
    def toNative = l.map(_.toNative)
  }
*/
}