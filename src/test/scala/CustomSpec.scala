package io.tabmo.aerospike

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures

trait CustomSpec extends WordSpec with ScalaFutures {

  private val timeout = 5.seconds
  def ready[T](f: Future[T]) = Await.ready(f, timeout)
  def result[T](f: Future[T]) = Await.result(f,timeout)

}
