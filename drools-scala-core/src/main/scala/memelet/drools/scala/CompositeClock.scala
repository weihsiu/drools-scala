package memelet.drools.scala

import java.util.concurrent.TimeUnit
import org.joda.time.{Period, DateTime, DateTimeUtils}
import org.drools.time.{TimerService, SessionPseudoClock}
import org.drools.time.impl.PseudoClockScheduler

// TODO Move to a common projects

object CompositeClock {
  implicit def toTimerService(ic: CompositeClock): TimerService = ic.droolsClock
}

class CompositeClock(private val droolsClock: PseudoClockScheduler) {

  DateTimeUtils.setMillisProvider(new DateTimeUtils.MillisProvider {
    override def getMillis = droolsClock.getCurrentTime
  });
  
  def currentTime = new DateTime(droolsClock.getCurrentTime)

  def advanceBy(amount: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): DateTime = new DateTime(droolsClock.advanceTime(amount, unit))
  def advanceBy(amount: Period): DateTime = advanceBy(amount.toStandardDuration.getMillis)

  def advanceTo(time: DateTime): DateTime = advanceBy(time.getMillis - droolsClock.getCurrentTime())

  def withTime(time: DateTime): CompositeClock = {
    advanceTo(time)
    this
  }

}

