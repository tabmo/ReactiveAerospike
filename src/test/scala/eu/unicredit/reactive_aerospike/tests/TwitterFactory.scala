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

package eu.unicredit.reactive_aerospike.tests

import eu.unicredit.reactive_aerospike.future._
object TwitterFactory extends Factory {
  class TwitterFuture[+T](f: com.twitter.util.Future[T])
      extends Future[T] {
    val inner = f

    def map[S](f: T => S): Future[S] = {
      val p = new TwitterPromise[S]
      inner.onSuccess { value =>
        try
          p.success(f(value))
        catch {
          case err: Throwable =>
            p.failure(err)
        }
      }
      inner.onFailure { err => p.failure(err) }
      p.future
    }
    def flatMap[S](f: T => Future[S]): Future[S] = {
      val p = new TwitterPromise[S]
      inner.onSuccess { value =>
        try
          f(value).map(x => { p.success(x) })
        catch {
          case err: Throwable =>
            p.failure(err)
        }
      }
      inner.onFailure { err => p.failure(err) }
      p.future
    }
  }

  class TwitterPromise[T] extends Promise[T] {
    val inner = com.twitter.util.Promise.apply[T]()
    def future: Future[T] = new TwitterFuture(inner.interruptible)
    def success(value: T): Unit = inner.setValue(value)
    def failure(exception: Throwable): Unit = inner.raise(exception)
  }

  def newPromise[T] = new TwitterPromise[T]

  object Helpers {

    implicit def fromTFToFuture[T](x: Future[T]): com.twitter.util.Future[T] =
      x match {
        case sf: TwitterFuture[T] =>
          sf.inner
        case _ => throw new Exception("Wrong future type")
      }
    implicit def fromFutureToSF[T](x: com.twitter.util.Future[T]): TwitterFuture[T] =
      new TwitterFuture(x)

  }
}