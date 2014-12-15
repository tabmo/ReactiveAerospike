package eu.unicredit.reactive_aerospike.data

import com.aerospike.client.Value
import com.aerospike.client.Value._
import com.aerospike.client.util.Packer
import com.aerospike.client.lua.LuaInstance
import scala.collection.JavaConverters._

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
  
  trait AerospikeValueConverter[T] {
    def toAsV(x: T): AerospikeValue[T]
    def fromValue(x: Value): AerospikeValue[T]
  }
  
  case class AerospikeNull()
      extends AerospikeValue[Null] {
    override val inner = new NullValue
  }
  
  implicit object AerospikeNullReader extends AerospikeValueConverter[Null] {
    def toAsV(n: Null): AerospikeNull = AerospikeNull()
    def fromValue(vi: Value): AerospikeNull = AerospikeNull() 
  }
  
  case class AerospikeInt(i: Int)
      extends AerospikeValue[Int] {
    override val inner = new LongValue(i)
  }
  
  implicit object AerospikeIntReader extends AerospikeValueConverter[Int] {
    def toAsV(i: Int): AerospikeInt = AerospikeInt(i)
    def fromValue(vi: Value): AerospikeInt = AerospikeInt(vi.toInteger()) 
  }
  
  case class AerospikeLong(l: Long)
  	  extends AerospikeValue[Long] {
    override val inner = new LongValue(l)
  }
  
  implicit object AerospikeLongReader extends AerospikeValueConverter[Long] {
    def toAsV(i: Long): AerospikeLong = AerospikeLong(i)
    def fromValue(vi: Value): AerospikeLong = AerospikeLong(vi.toLong()) 
  }
  
  case class AerospikeDouble(d: Double)
  	  extends AerospikeValue[Double] {
    override val inner = new LongValue(java.lang.Double.doubleToLongBits(d))
    
    override def toString() =
    	d.toString
  }
  
  implicit object AerospikeDoubleReader extends AerospikeValueConverter[Double] {
    def toAsV(d: Double): AerospikeDouble = AerospikeDouble(d)
    def fromValue(vd: Value): AerospikeDouble = AerospikeDouble(java.lang.Double.longBitsToDouble(vd.toLong())) 
  }
  
  case class AerospikeString(s: String)
      extends AerospikeValue[String] {
    override val inner = new StringValue(s)
  }
  
  implicit object AerospikeStringReader extends AerospikeValueConverter[String] {
    def toAsV(s: String): AerospikeString = AerospikeString(s)
    def fromValue(vs: Value): AerospikeString = AerospikeString(vs.toString)  
  }
  
  case class AerospikeBlob(b: Array[Byte])
      extends AerospikeValue[Array[Byte]] {
    override val inner = new BlobValue(b)
  }
  
  implicit object AerospikeBlobReader extends AerospikeValueConverter[Array[Byte]] {
    def toAsV(ab: Array[Byte]): AerospikeBlob = AerospikeBlob(ab)
    def fromValue(vb: Value): AerospikeBlob = AerospikeBlob(vb.getObject().asInstanceOf[Array[Byte]])  
  }
  
  case class AerospikeList[+T <: Any](l: List[AerospikeValue[T]])
      extends AerospikeValue[List[AerospikeValue[T]]] {
    override val inner = new ListValue(l.asJava)
    
    def this(elems: AerospikeValue[T]*) =
      	this(elems.toList)
  }
  
  /* helper constructor */
  object AerospikeList {
    def apply[T <: Any](values : AerospikeValue[T]*): AerospikeList[T] =
      AerospikeList(values.toList)
  }
  
  implicit def listReader[T <: Any](implicit reader: AerospikeValueConverter[T]) = {
	AerospikeListReader[T]()
  }
  
  case class AerospikeListReader[T <: Any](implicit reader: AerospikeValueConverter[T]) 
  	extends AerospikeValueConverter[List[AerospikeValue[T]]] {
    def toAsV(l: List[AerospikeValue[T]]): AerospikeList[T] = 
      AerospikeList(l)
    def fromValue(vl: Value): AerospikeList[T] = {
      try {
    	  val listRaw =
    		vl.getObject() match {
    	    	case _listRaw: java.util.List[_] => _listRaw.asScala.toList
    	    	case _ => throw new Exception("Data is not a list")
    	  }
    	  val result = listRaw.map(elem => reader.fromValue(Value.get(elem))) 	  
    	  AerospikeList(result)
      } catch {
        case err: Throwable => 
          	throw new Exception("Cannot parse list element")
      }
    }
  }
  
  def apply[T <: Any]
		  	(x: Value)
  			(implicit conv: AerospikeValueConverter[T]): AerospikeValue[T] = {
	  conv.fromValue(x)
  }
  
  /*
  def apply[T](x: Object, conv: AerospikeValueConverter[T]): AerospikeValue[T] = {
	  conv.toAsV(x.asInstanceOf[T])
  }
   */
  /* now using implicit converters
  def apply(x: Any): AerospikeValue[_] = {
    if (x == null) AerospikeNull()
    else
      x match {
      	case av: AerospikeValue[_] => av
        case s: String => AerospikeString(s)
        case i: Int => AerospikeInteger(i)
        case l: Long => AerospikeInteger(l)
        case any =>
          	println(s"Default match not found! ${any} ${any.getClass}")
          	AerospikeNull()
      }
  }
  */
  
  /*
  def apply[T](x: T)
  			(implicit conv: AerospikeValueConverter[T]): AerospikeValue[T] = {
	  conv.toAsV(x)
  }
  */
  
  //  implicit def fromObjectToNull(o: java.lang.Object) =
  //    AerospikeNull

  //  implicit def fromObjectToString(o: java.lang.Object) =
  //    AerospikeString(o.asInstanceOf[String])

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