package validation

import com.aerospike.client.Value.{ListValue, LongValue, MapValue, StringValue}
import org.scalatest.{FlatSpec, Matchers}
import io.tabmo.aerospike.validation.AsEncoder
import jto.validation.aerospike._

class AsEncoderSpec extends FlatSpec with Matchers {

  "AsEncoder" should "encode long value" in {
    AsEncoder[Long].encode(1L) shouldBe AsValue.long(1L)
  }

  "AsEncoder" should "encode string value" in {
    AsEncoder[String].encode("foo") shouldBe AsValue.string("foo")
  }

  "AsEncoder" should "encode option value" in {
    AsEncoder[Option[String]].encode(Some("foo")) shouldBe AsValue.string("foo")
    AsEncoder[Option[String]].encode(None) shouldBe AsNull
  }

  "AsEncoder" should "encode list value" in {
    AsEncoder[List[String]].encode(List("foo")) shouldBe AsValue.arr(AsValue.string("foo"))
  }

  "AsEncoder" should "encode object value" in {

    case class Contact(address: String)
    case class Person(name: String, age: Long, contact: Contact, friends: Option[List[String]])

    val encodedObject = AsEncoder[Person].encode(Person("Romain", 27, Contact("Rue de Thor"), Some(List("toto", "fifou"))))

    encodedObject shouldBe AsValue.obj(
      "name" -> AsString("Romain"),
      "age" -> AsLong(27),
      "contact" -> AsValue.obj("address" -> AsString("Rue de Thor")),
      "friends" -> AsArray(Array(AsString("toto"), AsString("fifou")))
    )

    val binSeq = encodedObject.asObject.toSeqBins //asObject is an unsafe operation
    binSeq.map(_.name) shouldBe List("name", "age", "contact", "friends")

    binSeq.find(_.name == "name").get.value shouldBe a[StringValue]
    binSeq.find(_.name == "age").get.value shouldBe a[LongValue]
    binSeq.find(_.name == "contact").get.value shouldBe a[MapValue]
    binSeq.find(_.name == "friends").get.value shouldBe a[ListValue]
  }

  "AsEncoder" should "be contramap" in {
    val booleanEncoder = AsEncoder[Long].contramap[Boolean](b => if (b) 1L else 0L)
    booleanEncoder.encode(true) shouldBe AsValue.long(1L)
    booleanEncoder.encode(false) shouldBe AsValue.long(0L)
  }

}
