package jto.validation
package aerospike

import scala.language.implicitConversions

object Rules extends DefaultRules[AsValue] {

  private def recordAs[T](f: PartialFunction[AsValue, Validated[Seq[ValidationError], T]])(msg: String, args: Any*) =
    Rule.fromMapping[AsValue, T](f.orElse {
      case j => Invalid(Seq(ValidationError(msg, args: _*)))
    })

  implicit def asObjectR =
    recordAs[AsObject] {
      case v@AsObject(_) => Valid(v)
    }("error.invalid", "Object")

  implicit def asStringR =
    recordAs[AsString] {
      case v@AsString(_) => Valid(v)
    }("error.invalid", "AsString")

  implicit def asLongR =
    recordAs[AsLong] {
      case v@AsLong(_) => Valid(v)
    }("error.invalid", "AsLong")

  implicit def asArrayR =
    recordAs[AsArray] {
      case v@AsArray(_) => Valid(v)
    }("error.invalid", "AsArray")

  implicit def intR =
    recordAs[Int] {
      case AsLong(v) => Valid(v.toInt)
    }("error.invalid", "Int")

  implicit def stringR =
    recordAs[String] {
      case AsString(v) => Valid(v)
    }("error.invalid", "String")

  implicit def longR =
    recordAs[Long] {
      case AsLong(v) => Valid(v)
    }("error.invalid", "Long")

  implicit def bigDecimalR =
    recordAs[BigDecimal] {
      case AsLong(v) => Valid(v)
    }("error.number", "BigDecimal")

  implicit val asNullR: Rule[AsValue, AsNull.type] = recordAs[AsNull.type] {
    case AsNull => Valid(AsNull)
  }("error.invalid", "null")

  implicit def mapR[O](implicit r: RuleLike[AsValue, O]): Rule[AsValue, Map[String, O]] =
    super.mapR[AsValue, O](r, asObjectR.map { case AsObject(fs) => fs.toSeq })

  def optionR[AS, O](r: => Rule[AS, O], noneValues: Rule[AsValue, AsValue]*)(implicit pick: Path => Rule[AsValue, AsValue], coerce: Rule[AsValue, AS]): Path => Rule[AsValue, Option[O]]
  = super.opt[AS, O](r, asNullR.map(n => n: AsValue) +: noneValues: _*)

  implicit def pickInRecord[II <: AsValue, O](p: Path)(implicit r: RuleLike[AsValue, O]): Rule[II, O] = {
    def search(p: Path, asValue: AsValue): Option[AsValue] = {
      p.path match {
        case KeyPathNode(k) :: t =>
          asValue match {
            case AsObject(as) => as.get(k).flatMap(search(Path(t), _))
            case _ => None
          }
        case IdxPathNode(i) :: t =>
          asValue match {
            case AsArray(as) => as.lift(i).flatMap(search(Path(t), _))
            case _ => None
          }
        case Nil => Some(asValue)
      }
    }

    Rule[II, AsValue] { asValue =>
      search(p, asValue) match {
        case None =>
          Invalid(Seq(Path -> Seq(ValidationError("error.required"))))
        case Some(as) => Valid(as)
      }
    }.andThen(r)
  }

  private def pickInS[T](implicit r: RuleLike[Seq[AsValue], T]): Rule[AsValue, T] =
    asArrayR.map { case AsArray(fs) => fs: Seq[AsValue] }.andThen(r)
  implicit def pickSeq[O](implicit r: RuleLike[AsValue, O]) =
    pickInS(seqR[AsValue, O])
  implicit def pickSet[O](implicit r: RuleLike[AsValue, O]) =
    pickInS(setR[AsValue, O])
  implicit def pickList[O](implicit r: RuleLike[AsValue, O]) =
    pickInS(listR[AsValue, O])
  implicit def pickArray[O: scala.reflect.ClassTag](implicit r: RuleLike[AsValue, O]) = pickInS(arrayR[AsValue, O])
  implicit def pickTraversable[O](implicit r: RuleLike[AsValue, O]) =
    pickInS(traversableR[AsValue, O])
}
