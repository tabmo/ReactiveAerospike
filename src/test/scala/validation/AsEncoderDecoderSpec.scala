package validation

import com.aerospike.client.Value.{ListValue, LongValue, MapValue, StringValue}
import org.scalatest.{FlatSpec, Matchers}
import io.tabmo.aerospike.validation.{AsDecoder, AsEncoder, Done}
import jto.validation.aerospike._

class AsEncoderDecoderSpec extends FlatSpec with Matchers {

  case class Contact(address: String)

  case class Person(name: String, age: Long, contact: Contact, friends: Option[List[String]])

  "AsValue" should "well decode to object with AsDecoder" in {

    val personAs = AsValue.obj(
      "name" -> AsString("Romain"),
      "age" -> AsLong(27),
      "contact" -> AsValue.obj("address" -> AsString("Rue de Thor")),
      "friends" -> AsArray(Array(AsString("toto"), AsString("fifou")))
    )

    AsDecoder[Person].decode(personAs) shouldBe Done(Person("Romain", 27, Contact("Rue de Thor"), Some(List("toto", "fifou"))))
  }

  "AsValue" should "well encode from object with AsEncoder" in {

    val asObject = AsEncoder[Person].encode(Person("Romain", 27, Contact("Rue de Thor"), Some(List("toto", "fifou"))))

    asObject shouldBe AsValue.obj(
      "name" -> AsString("Romain"),
      "age" -> AsLong(27),
      "contact" -> AsValue.obj("address" -> AsString("Rue de Thor")),
      "friends" -> AsArray(Array(AsString("toto"), AsString("fifou")))
    )

    val binSeq = asObject.asObject.toSeqBins
    binSeq.map(_.name) shouldBe List("name", "age", "contact", "friends")

    binSeq.find(_.name == "name").get.value shouldBe a[StringValue]
    binSeq.find(_.name == "age").get.value shouldBe a[LongValue]
    binSeq.find(_.name == "contact").get.value shouldBe a[MapValue]
    binSeq.find(_.name == "friends").get.value shouldBe a[ListValue]
  }

}
