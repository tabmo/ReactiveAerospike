package io.tabmo.aerospike.utils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.BigDecimal.RoundingMode

import org.slf4j.LoggerFactory

/**
 * Lazy logger
 */
class Logger(logger: org.slf4j.Logger) {

  private val idGenerator = new java.util.concurrent.atomic.AtomicLong()

  def timing[T](key: String)(f: Future[T]): Future[T] = {
    val id = idGenerator.incrementAndGet()

    logger.debug(s"[$id] $key")
    val start = System.nanoTime()

    if (logger.isTraceEnabled) {
      f.onComplete {
        case _ =>
          val duration = (BigDecimal(System.nanoTime() - start) / 1000000).setScale(2, RoundingMode.HALF_UP)
          logger.trace(s"[$id] complete in ${duration}ms")
      }
    }

    f
  }

  /**
   * `true` if the logger instance is enabled for the `TRACE` level.
   */
  def isTraceEnabled = logger.isTraceEnabled

  /**
   * `true` if the logger instance is enabled for the `DEBUG` level.
   */
  def isDebugEnabled = logger.isDebugEnabled

  /**
   * `true` if the logger instance is enabled for the `INFO` level.
   */
  def isInfoEnabled = logger.isInfoEnabled

  /**
   * `true` if the logger instance is enabled for the `WARN` level.
   */
  def isWarnEnabled = logger.isWarnEnabled

  /**
   * `true` if the logger instance is enabled for the `ERROR` level.
   */
  def isErrorEnabled = logger.isErrorEnabled

  /**
   * Logs a message with the `TRACE` level.
   *
   * @param message the message to log
   */
  def trace(message: => String) {
    if (logger.isTraceEnabled) logger.trace(message)
  }

  /**
   * Logs a message with the `TRACE` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def trace(message: => String, error: => Throwable) {
    if (logger.isTraceEnabled) logger.trace(message, error)
  }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   */
  def debug(message: => String) {
    if (logger.isDebugEnabled) logger.debug(message)
  }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def debug(message: => String, error: => Throwable) {
    if (logger.isDebugEnabled) logger.debug(message, error)
  }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   */
  def info(message: => String) {
    if (logger.isInfoEnabled) logger.info(message)
  }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def info(message: => String, error: => Throwable) {
    if (logger.isInfoEnabled) logger.info(message, error)
  }

  /**
   * Logs a message with the `WARN` level.
   *
   * @param message the message to log
   */
  def warn(message: => String) {
    if (logger.isWarnEnabled) logger.warn(message)
  }

  /**
   * Logs a message with the `WARN` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def warn(message: => String, error: => Throwable) {
    if (logger.isWarnEnabled) logger.warn(message, error)
  }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   */
  def error(message: => String) {
    if (logger.isErrorEnabled) logger.error(message)
  }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def error(message: => String, error: => Throwable) {
    if (logger.isErrorEnabled) logger.error(message, error)
  }

}

object Logger {

  /**
   * Return a logger named according to the name parameter using the statically
   * bound instance.
   *
   * @param name The name of the logger.
   * @return logger
   */
  def getInstance(name: String) = new Logger(LoggerFactory.getLogger(name))

  /**
   * Return a logger named corresponding to the class passed as parameter, using
   * the statically bound instance.
   *
   * @param clazz the returned logger will be named after clazz
   * @return logger
   */
  def getInstance(clazz: Class[_]) = new Logger(LoggerFactory.getLogger(clazz))
}
