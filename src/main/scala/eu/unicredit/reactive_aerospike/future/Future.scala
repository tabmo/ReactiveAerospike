package eu.unicredit.reactive_aerospike.future

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

trait Future[+T]  {
  def map[S](f: T => S)(implicit executionContext: ExecutionContext): Future[S]
  def flatMap[S]
	(f: T => Future[S])(implicit executionContext: ExecutionContext): Future[S]
  
  /* to add onComplete helpers*/
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
	import scala.util.{Success, Failure}
    class ScalaFuture[+T]
		  (f: scala.concurrent.Future[T])
		  extends Future[T] {
    val inner = f
    
    def map[S](f: T => S)
    		  (implicit executionContext: ExecutionContext)
    		  : Future[S] = {
    	val p = new ScalaPromise[S]
    	inner.onComplete{ 
    	  case Success(value) => 
          	p.success(f(value))
    	  case Failure(err) => 
          	p.failure(err)
    	}
    	p.future
    }
  	def flatMap[S](f: T => Future[S])
  				(implicit executionContext: ExecutionContext)
  				: Future[S] = {
      val p = new ScalaPromise[S]
      inner.onComplete{
        case Success(value) => 
          	f(value).flatMap(x => {p.success(x); p.future})
        case Failure(err) => p.failure(err)
      }
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
    
    implicit def fromSFToFuture[T](x: Future[T])
    			: scala.concurrent.Future[T] =
    			x match {
    				case sf: ScalaFuture[T] =>
    				  	sf.inner
    				case _ => throw new Exception("Wrong future type")
    			}
    implicit def fromFutureToSF[T](x: scala.concurrent.Future[T])
    			: ScalaFuture[T] =
    			new ScalaFuture(x) 
        
  }
}

/* Twitter Future compatibility implementation */
object TwitterFactory extends Factory {
	class TwitterFuture[+T]
		  (f: com.twitter.util.Future[T])
		  extends Future[T] {
    val inner = f
    
    def map[S](f: T => S)
    		  (implicit executionContext: ExecutionContext)
    		  : Future[S] = {
    	val p = new TwitterPromise[S]
    	inner.onSuccess{value => p.success(f(value))}
    	inner.onFailure{err => p.failure(err)}
    	p.future
    }
  	def flatMap[S](f: T => Future[S])
  				(implicit executionContext: ExecutionContext)
  				: Future[S] = {
      val p = new TwitterPromise[S]
      inner.onSuccess{value => f(value).flatMap(x => {p.success(x); p.future})}
      inner.onFailure{err => p.failure(err)}
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
    
    implicit def fromTFToFuture[T](x: Future[T])
    			: com.twitter.util.Future[T] =
    			x match {
    				case sf: TwitterFuture[T] =>
    				  	sf.inner
    				case _ => throw new Exception("Wrong future type")
    			}
    implicit def fromFutureToSF[T](x: com.twitter.util.Future[T])
    			: TwitterFuture[T] =
    			new TwitterFuture(x) 
        
  }
}
