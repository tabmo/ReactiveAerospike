/* Copyright 2014 UniCredit S.p.A.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package eu.unicredit.reactive_aerospike.data

import scala.util.control.NonFatal

import org.luaj.vm2.LuaValue
import com.aerospike.client.Value
import com.aerospike.client.Value._
import com.aerospike.client.util.Packer
import com.aerospike.client.lua.LuaInstance
import com.aerospike.client.command.ParticleType

import scala.collection.JavaConverters._

trait AerospikeValue[+T <: Any] extends Value {
  val inner: Value
  val base: T

  override def estimateSize(): Int = inner.estimateSize()

  override def write(buffer: Array[Byte], offset: Int): Int = inner.write(buffer, offset)

  override def pack(packer: Packer): Unit = inner.pack(packer)

  override def getType: Int = inner.getType

  override def getObject: Object = inner.getObject

  override def getLuaValue(instance: LuaInstance): LuaValue = inner.getLuaValue(instance)

  override def toString: String = inner.toString
}

object AerospikeValue {

  def tryo[T](x: => T): Option[T] =
    try Some(x)
    catch {
      case NonFatal(_) => None
    }

  trait AerospikeValueConverter[T] {
    def toAsV(x: T): AerospikeValue[T]
    def fromValue(x: Value): AerospikeValue[T]
  }

  case class AerospikeNull() extends AerospikeValue[Null] {
    override val inner = new NullValue
    override val base = null
  }

  implicit object AerospikeNullConverter extends AerospikeValueConverter[Null] {
    def toAsV(n: Null): AerospikeNull = AerospikeNull()
    def fromValue(vn: Value): AerospikeNull = {
      assert(vn.getType == ParticleType.NULL)
      AerospikeNull()
    }
  }

  case class AerospikeInt(i: Int) extends AerospikeValue[Int] {
    override val inner = new LongValue(i.toLong)
    override val base = i
  }

  implicit object AerospikeIntConverter extends AerospikeValueConverter[Int] {
    def toAsV(i: Int): AerospikeInt = AerospikeInt(i)
    def fromValue(vi: Value): AerospikeInt = {
      assert(vi.getType == ParticleType.INTEGER)
      AerospikeInt(vi.getObject.asInstanceOf[Long].toInt)
    }
  }

  case class AerospikeLong(l: Long) extends AerospikeValue[Long] {
    override val inner = new LongValue(l)
    override val base = l
  }

  implicit object AerospikeLongConverter extends AerospikeValueConverter[Long] {
    def toAsV(i: Long): AerospikeLong = AerospikeLong(i)
    def fromValue(vi: Value): AerospikeLong = {
      assert(vi.getType == ParticleType.INTEGER)
      AerospikeLong(vi.getObject.asInstanceOf[Long])
    }
  }

  case class AerospikeDouble(d: Double) extends AerospikeValue[Double] {
    override val inner = new LongValue(java.lang.Double.doubleToLongBits(d))
    override val base = d
    override def toString: String = d.toString
  }

  implicit object AerospikeDoubleConverter extends AerospikeValueConverter[Double] {
    def toAsV(d: Double): AerospikeDouble = AerospikeDouble(d)
    def fromValue(vd: Value): AerospikeDouble = {
      assert(vd.getType == ParticleType.INTEGER)
      AerospikeDouble(java.lang.Double.longBitsToDouble(vd.getObject.asInstanceOf[Long]))
    }
  }

  case class AerospikeString(s: String) extends AerospikeValue[String] {
    override val inner = new StringValue(s)
    override val base = s
  }

  implicit object AerospikeStringConverter extends AerospikeValueConverter[String] {
    def toAsV(s: String): AerospikeString = AerospikeString(s)
    def fromValue(vs: Value): AerospikeString = {
      assert(vs.getType == ParticleType.STRING)
      AerospikeString(vs.toString)
    }
  }

  case class AerospikeBlob(b: Array[Byte]) extends AerospikeValue[Array[Byte]] {
    override val inner = new BlobValue(b)
    override val base = b
  }

  implicit object AerospikeBlobConverter extends AerospikeValueConverter[Array[Byte]] {
    def toAsV(ab: Array[Byte]): AerospikeBlob = AerospikeBlob(ab)
    def fromValue(vb: Value): AerospikeBlob = {
      AerospikeBlob(vb.getObject.asInstanceOf[Array[Byte]])
    }
  }

  case class AerospikeList[+T <: Any](l: List[AerospikeValue[T]]) extends AerospikeValue[List[AerospikeValue[T]]] {
    override val inner = new ListValue(l.asJava)
    override val base = l

    def this(elems: AerospikeValue[T]*) = this(elems.toList)
  }

  object AerospikeList {
    def apply[T <: Any](values: AerospikeValue[T]*): AerospikeList[T] = AerospikeList(values.toList)
  }

  implicit def listConverter[T <: Any](implicit converter: AerospikeValueConverter[T]): AerospikeListConverter[T] = {
    AerospikeListConverter[T]()
  }

  case class AerospikeListConverter[T <: Any](implicit converter: AerospikeValueConverter[T]) extends AerospikeValueConverter[List[AerospikeValue[T]]] {
    def toAsV(l: List[AerospikeValue[T]]): AerospikeList[T] = AerospikeList(l)
    def fromValue(vl: Value): AerospikeList[T] = {
      val listRaw = vl.getObject match {
        case _listRaw: java.util.List[_] => _listRaw.asScala.toList
        case _ => throw new Exception("Data is not a list")
      }
      val result = listRaw.map(elem => converter.fromValue(Value.get(elem)))
      AerospikeList(result)
    }
  }

  case class AerospikeMap[T1 <: Any, T2 <: Any](m: Map[AerospikeValue[T1], AerospikeValue[T2]]) extends AerospikeValue[Map[AerospikeValue[T1], AerospikeValue[T2]]] {
    override val inner = new MapValue(m.asJava)
    override val base = m
  }

  object AerospikeMap {
    def apply[T1 <: Any, T2 <: Any](values: (AerospikeValue[T1], AerospikeValue[T2])*): AerospikeMap[T1, T2] = AerospikeMap(values.toMap)
    def apply[T1 <: Any, T2 <: Any](values: (T1, T2)*)(
      implicit converter1: AerospikeValueConverter[T1],
      converter2: AerospikeValueConverter[T2]): AerospikeMap[T1, T2] =
      AerospikeMap(values.map(x => converter1.toAsV(x._1) -> converter2.toAsV(x._2)).toMap)
  }

  implicit def mapConverter[T1 <: Any, T2 <: Any](implicit converter1: AerospikeValueConverter[T1],
    converter2: AerospikeValueConverter[T2]): AerospikeMapConverter[T1, T2] = {
    AerospikeMapConverter[T1, T2]()
  }

  sealed class AerospikeTuple[T1 <: Any, T2 <: Any](x1: T1, x2: T2) extends AerospikeValue[(T1, T2)] {
    override val inner = new NullValue
    override val base = (x1, x2)
    val key = x1
    val value = x2
  }

  sealed class AerospikeTupleConverter[T1 <: Any, T2 <: Any](implicit converter1: AerospikeValueConverter[T1], converter2: AerospikeValueConverter[T2]) extends AerospikeValueConverter[(T1, T2)] {
    def toAsV(t: (T1, T2)): AerospikeTuple[T1, T2] = new AerospikeTuple(t._1, t._2)
    def fromValue(vb: Value): AerospikeTuple[T1, T2] =
      throw new RuntimeException("Please parse separately key and value")
  }

  case class AerospikeMapConverter[T1 <: Any, T2 <: Any](implicit converter1: AerospikeValueConverter[T1], converter2: AerospikeValueConverter[T2]) extends AerospikeValueConverter[Map[AerospikeValue[T1], AerospikeValue[T2]]] {
    def toAsV(m: Map[AerospikeValue[T1], AerospikeValue[T2]]): AerospikeMap[T1, T2] = AerospikeMap(m)
    def fromValue(vm: Value): AerospikeMap[T1, T2] = {
      val mapRaw =
        vm.getObject match {
          case _mapRaw: java.util.Map[_, _] => _mapRaw.asScala.toMap
          case _ => throw new Exception("Data is not a map")
        }
      val result = mapRaw.map(elem => converter1.fromValue(Value.get(elem._1)) -> converter2.fromValue(Value.get(elem._2)))
      AerospikeMap(result)
    }
  }

  def apply[T <: Any](x: Value)(implicit conv: AerospikeValueConverter[T]): AerospikeValue[T] = conv.fromValue(x)

}
