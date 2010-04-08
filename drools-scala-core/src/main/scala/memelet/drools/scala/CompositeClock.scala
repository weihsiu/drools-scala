package org.joda.time {

  import org.drools.time.impl.PseudoClockScheduler

  class DroolsMillisProvider(droolsClock: PseudoClockScheduler) extends DateTimeUtils.MillisProvider {
    override def getMillis = droolsClock.getCurrentTime
  }

}

package memelet.drools.scala {

  import java.util.concurrent.TimeUnit
  import org.joda.time.{Period, DateTime, DateTimeUtils, DroolsMillisProvider}
  import org.drools.time.TimerService
  import org.drools.time.impl.PseudoClockScheduler

  object CompositeClock {
    implicit def toTimerService(ic: CompositeClock): TimerService = ic.droolsClock
  }

  class CompositeClock(private val droolsClock: PseudoClockScheduler) {

    // Inject the drools millis provider into jodatime
    val cMillisProvider = classOf[DateTimeUtils].getDeclaredField("cMillisProvider")
    cMillisProvider.setAccessible(true)
    cMillisProvider.set(null, new DroolsMillisProvider(droolsClock))

    def currentTime = new DateTime(droolsClock.getCurrentTime)

    def advanceBy(amount: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): DateTime = new DateTime(droolsClock.advanceTime(amount, unit))
    def advanceBy(amount: Period): DateTime = advanceBy(amount.toStandardDuration.getMillis)

    def advanceTo(time: DateTime): DateTime = advanceBy(time.getMillis - droolsClock.getCurrentTime())

    def withTime(time: DateTime): CompositeClock = {
      advanceTo(time)
      this
    }

  }
}
