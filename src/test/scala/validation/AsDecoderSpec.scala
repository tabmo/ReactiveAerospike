package validation

import org.scalatest.{FlatSpec, Matchers}
import io.tabmo.aerospike.validation.{AsDecoder, Done}
import jto.validation.aerospike._

class AsDecoderSpec extends FlatSpec with Matchers {

  "AsDecoder" should "decode long value" in {
    AsDecoder[Long].decode(AsValue.long(1L)) shouldBe Done(1L)
  }

  "AsDecoder" should "decode string value" in {
    AsDecoder[String].decode(AsValue.string("foo")) shouldBe Done("foo")
  }

  "AsDecoder" should "decode option value" in {
    AsDecoder[Option[String]].decode(AsValue.string("foo")) shouldBe Done(Some("foo"))
    AsDecoder[Option[String]].decode(AsNull) shouldBe Done(None)
  }

  "AsDecoder" should "decode list value" in {
    AsDecoder[List[String]].decode(AsValue.arr(AsString("foo"), AsString("bar"))) shouldBe Done(List("foo", "bar"))
  }

  "AsDecoder" should "decoder object value" in {
    case class Contact(address: String)
    case class Person(name: String, age: Long, contact: Contact, friends: Option[List[String]])

    val personAs = AsValue.obj(
      "name" -> AsString("Romain"),
      "age" -> AsLong(27),
      "contact" -> AsValue.obj("address" -> AsString("Rue de Thor")),
      "friends" -> AsArray(Array(AsString("toto"), AsString("fifou")))
    )

    AsDecoder[Person].decode(personAs) shouldBe Done(Person("Romain", 27, Contact("Rue de Thor"), Some(List("toto", "fifou"))))
  }

  "AsDecoder" should "be map" in {
    val booleanDecoder = AsDecoder[Long].map(l => if (l > 0) true else false)
    booleanDecoder.decode(AsValue.long(1L)) shouldBe Done(true)
    booleanDecoder.decode(AsValue.long(0L)) shouldBe Done(false)
  }
}
