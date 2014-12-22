package eu.unicredit.reactive_aerospike.future

import scala.language.higherKinds
import javax.xml.datatype.Duration

trait Future[+T]  {
  def map[S](f: T => S): Future[S]
  def flatMap[S]
	(f: T => Future[S]): Future[S]
  
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
    
    def map[S](f: T => S)
    		  : Future[S] = {
        implicit val ec = implicitly[scala.concurrent.ExecutionContext]
    	val p = new ScalaPromise[S]
    	inner.onComplete{ 
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
  	def flatMap[S](f: T => Future[S])
  				: Future[S] = {
  	  implicit val ec = implicitly[scala.concurrent.ExecutionContext]
      val p = new ScalaPromise[S]
      inner.onComplete{
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
    		  : Future[S] = {
    	val p = new TwitterPromise[S]
   		inner.onSuccess{value =>
   		  try
   		  	p.success(f(value))
   		  catch {
    		  	case err: Throwable =>
    		  		p.failure(err)
    	  }
   		}
    	inner.onFailure{err => p.failure(err)}
    	p.future
    }
  	def flatMap[S](f: T => Future[S])
  				: Future[S] = {
      val p = new TwitterPromise[S]
      inner.onSuccess{value => 
        try
        	f(value).map(x => {p.success(x)})
        catch {
        	case err: Throwable => 
        		p.failure(err)
        }
      }
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
