package io.tabmo.aerospike

object TSafe {

  @annotation.implicitNotFound(
    msg = "ReactiveAerospike support only String and Long, but you provide a ${V}:"
  )
  sealed class VRestriction[V]
  object VRestriction {
    implicit object int extends VRestriction[Int] // For convenient reasons, we also accept Int, but it will be transformed to Long
    implicit object long extends VRestriction[Long]
    implicit object string extends VRestriction[String]
  }
}
