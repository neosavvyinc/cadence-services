package com.cadence.framework

import org.slf4j.LoggerFactory

/**
 * Created by aparrish on 8/23/14.
 */
trait Logging {
  val log = LoggerFactory.getLogger(this.getClass)

  def debug[T <: AnyRef](t : T) = if (log.isDebugEnabled) log.debug(t.toString)
  def info[T <: AnyRef](t : T) = if (log.isInfoEnabled) log.info(t.toString)
}
