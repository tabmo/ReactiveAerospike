package validation

import org.scalatest.{Matchers, WordSpec}
import jto.validation._
import jto.validation.aerospike.Rules._
import jto.validation.aerospike._

class AerospikeRulesSpec extends WordSpec with Matchers {

  "Support all type of AsValue" when {

    "null" in {
      (Path \ "n")
        .read[AsValue, AsNull.type]
        .validate(AsValue.obj("n" -> AsNull)) shouldBe Valid(AsNull)
      (Path \ "n")
        .read[AsValue, AsNull.type]
        .validate(AsValue.obj("n" -> AsString("foo"))) shouldBe Invalid(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "null"))))
      (Path \ "n").read[AsValue, AsNull.type].validate(AsValue.obj("n" -> AsLong(4))) shouldBe Invalid(Seq(Path \ "n" -> Seq(ValidationError("error.invalid", "null"))))
    }

    "Int" in {
      (Path \ "n").read[AsValue, Int].validate(AsValue.obj("n" -> AsLong(4))) shouldBe Valid(4)

      (Path \ "n").read[AsValue, Int].validate(AsValue.obj("n" -> AsString("foo"))) shouldBe
        Invalid(Seq(Path \ "n" -> Seq(
          ValidationError("error.invalid", "Int"))))

      val errPath = Path \ "foo"
      val error = Invalid(Seq(errPath -> Seq(ValidationError("error.required"))))
      errPath.read[AsValue, Int].validate(AsValue.obj("n" -> AsLong(4))) shouldBe error
    }

    "Long" in {
      (Path \ "n").read[AsValue, Long].validate(AsValue.obj("n" -> AsLong(4))) shouldBe Valid(4L)

      (Path \ "n").read[AsValue, Long].validate(AsValue.obj("n" -> AsString("foo"))) shouldBe
        Invalid(Seq(Path \ "n" -> Seq(
          ValidationError("error.invalid", "Long"))))

      val errPath = Path \ "foo"
      val error = Invalid(Seq(errPath -> Seq(ValidationError("error.required"))))
      errPath.read[AsValue, Long].validate(AsValue.obj("n" -> AsLong(4))) shouldBe error
    }

    "String" in {
      (Path \ "n").read[AsValue, String].validate(AsValue.obj("n" -> AsString("foo"))) shouldBe Valid("foo")

      (Path \ "n").read[AsValue, String].validate(AsValue.obj("n" -> AsLong(1))) shouldBe
        Invalid(Seq(Path \ "n" -> Seq(
          ValidationError("error.invalid", "String"))))

      val errPath = Path \ "foo"
      val error = Invalid(Seq(errPath -> Seq(ValidationError("error.required"))))
      errPath.read[AsValue, Long].validate(AsValue.obj("n" -> AsString("foo"))) shouldBe error
    }

    "Seq" in {
      (Path \ "n")
        .read[AsValue, Seq[String]]
        .validate(AsValue.obj("n" -> AsValue.arr(AsString("foo"))))
        .toOption
        .get shouldBe Seq("foo")
    }

  }

}
