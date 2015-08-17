package io.tabmo.aerospike

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

trait CustomSpec extends WordSpec with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(15, Millis))

  private val timeout = 5.seconds
  def ready[T](f: Future[T]) = Await.ready(f, timeout)
  def result[T](f: Future[T]) = Await.result(f,timeout)

}
