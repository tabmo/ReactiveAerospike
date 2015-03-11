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

package eu.unicredit.reactive_aerospike.future

import scala.language.higherKinds

trait Future[+T] {
  def map[S](f: T => S): Future[S]
  def flatMap[S](f: T => Future[S]): Future[S]
}

trait Promise[T <: Any] {
  def future: Future[T]
  def success(value: T): Unit
  def failure(exception: Throwable): Unit
}

trait Factory {
  def newPromise[T]: Promise[T]
}

/* plain Scala default implementation */
object ScalaFactory extends Factory {
  import scala.util.{ Success, Failure }
  class ScalaFuture[+T](f: scala.concurrent.Future[T])
      extends Future[T] {
    val inner = f

    //please complete this before usage if needed

    lazy val executionContextPromise = scala.concurrent.Promise[scala.concurrent.ExecutionContext]
    lazy val defaultExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    private lazy implicit val executionContext =
      try
        scala.concurrent.Await.result(
          executionContextPromise.future, scala.concurrent.duration.Duration.Zero)
      catch {
        case _: Throwable => defaultExecutionContext
      }

    def map[S](f: T => S): Future[S] = {
      implicit val ec = implicitly[scala.concurrent.ExecutionContext]
      val p = new ScalaPromise[S]
      inner.onComplete {
        case Success(value) =>
          try
            p.success(f(value))
          catch {
            case err: Throwable =>
              p.failure(err)
          }
        case Failure(err) =>
          p.failure(err)
      }(ec)
      p.future
    }
    def flatMap[S](f: T => Future[S]): Future[S] = {
      implicit val ec = implicitly[scala.concurrent.ExecutionContext]
      val p = new ScalaPromise[S]
      inner.onComplete {
        case Success(value) =>
          try
            f(value).map(x => p.success(x))
          catch {
            case err: Throwable =>
              p.failure(err)
          }
        case Failure(err) => p.failure(err)
      }(ec)
      p.future
    }
  }

  class ScalaPromise[T] extends Promise[T] {
    val inner = scala.concurrent.Promise.apply[T]()
    def future: Future[T] = new ScalaFuture(inner.future)
    def success(value: T): Unit = inner.success(value)
    def failure(exception: Throwable): Unit = inner.failure(exception)
  }

  def newPromise[T] = new ScalaPromise[T]

  object Helpers {

    implicit def fromSFToFuture[T](x: Future[T]): scala.concurrent.Future[T] =
      x match {
        case sf: ScalaFuture[T] =>
          sf.inner
        case _ => throw new Exception("Wrong future type")
      }
    implicit def fromFutureToSF[T](x: scala.concurrent.Future[T]): ScalaFuture[T] =
      new ScalaFuture(x)

  }
}