package memelet.drools.scala

import org.drools.builder.{ResourceType, KnowledgeBuilderFactory}
import org.drools.io.{ResourceFactory, Resource}
import org.drools.runtime.conf.ClockTypeOption
import java.util.concurrent.TimeUnit
import org.drools.definition.KnowledgePackage
import scala.collection.JavaConversions._
import org.drools.event.rule._
import org.drools.runtime.{ObjectFilter, Globals, StatefulKnowledgeSession}
import org.drools.time.{TimerService, SessionPseudoClock, SessionClock}
import org.drools.conf.{KnowledgeBaseOption}
import org.joda.time.{Period, DateTime, DateTimeUtils}
import org.drools.{FactException, KnowledgeBaseFactory, KnowledgeBase}
import org.drools.runtime.rule.{AgendaFilter, FactHandle}

object DroolsBuilder {

  def buildKnowledgePackages(drlFilenames: Iterable[String]) : Iterable[KnowledgePackage] = {
    val kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder
    for (drlFilename <- drlFilenames) {
      val resource: Resource = ResourceFactory.newClassPathResource(drlFilename, getClass)
      kbuilder.add(resource, ResourceType.DRL)
    }
    if (kbuilder.hasErrors) {
      throw new RuntimeException(kbuilder.getErrors.toString)
    }
    kbuilder.getKnowledgePackages
  }

  def buildKnowledgeBase(kbaseOptions: KnowledgeBaseOption*): RichKnowledgeBase = buildKnowledgeBase(List.empty, kbaseOptions: _*)

  def buildKnowledgeBase(knowledgePackages: Iterable[KnowledgePackage], kbaseOptions: KnowledgeBaseOption*): RichKnowledgeBase = {
    val kbaseConfig = KnowledgeBaseFactory.newKnowledgeBaseConfiguration()
    kbaseOptions.foreach { kbaseConfig.setOption(_) }
    val kbase = KnowledgeBaseFactory.newKnowledgeBase(kbaseConfig)
    kbase.addKnowledgePackages(knowledgePackages)
    new RichKnowledgeBase(kbase)
  }
}

object RichDrools {

  def knowledgePackages(drlFilenames: Iterable[String]) : Iterable[KnowledgePackage] = {
    val kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder

    for (drlFilename <- drlFilenames) {
      val resource: Resource = ResourceFactory.newClassPathResource(drlFilename, getClass)
      kbuilder.add(resource, ResourceType.DRL)
    }
    if (kbuilder.hasErrors) {
      throw new RuntimeException(kbuilder.getErrors.toString)
    }

    kbuilder.getKnowledgePackages
  }
  def knowledgePackage(drlFilename: String) : Iterable[KnowledgePackage] = knowledgePackages(Seq(drlFilename))
  
  implicit def enrichKnowledgeBase(kbase: KnowledgeBase): RichKnowledgeBase = new RichKnowledgeBase(kbase)
  implicit def derichKnowledgeBase(rkbase: RichKnowledgeBase): KnowledgeBase = rkbase.kbase

  implicit def enrichSession(ksession: StatefulKnowledgeSession): RichStatefulKnowledgeSession = new RichStatefulKnowledgeSession(ksession)
  implicit def derichSession(rksession: RichStatefulKnowledgeSession): StatefulKnowledgeSession = rksession.session

  implicit def enrichGlobals(globals: Globals): RichGlobals = new RichGlobals(globals)
  implicit def enrichSessionPseudoClock(clock: SessionPseudoClock): RichSessionPseudoClock = new RichSessionPseudoClock(clock)
  
  val REALTIME_CLOCK_OPTION: ClockTypeOption = ClockTypeOption.get("realtime")
  val PSUEDO_CLOCK_OPTION: ClockTypeOption = ClockTypeOption.get("pseudo")
}

class RichKnowledgeBase(val kbase: KnowledgeBase) {

  def addKnowledgePackages(drlFilenames: Iterable[String]) {
    kbase.addKnowledgePackages(DroolsBuilder.buildKnowledgePackages(drlFilenames))
  }

  def statefulSession(implicit clockType: ClockTypeOption = RichDrools.REALTIME_CLOCK_OPTION) = {
    val ksessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration()
    ksessionConfig.setOption(clockType)
    kbase.newStatefulKnowledgeSession(ksessionConfig, null)
  }
}

class RichStatefulKnowledgeSession(val session: StatefulKnowledgeSession) {

  def knowledgeBase = session.getKnowledgeBase

  def addPackages(drlFilenames: Iterable[String]) {
    knowledgeBase.addKnowledgePackages(DroolsBuilder.buildKnowledgePackages(drlFilenames))
  }

  def addGlobal(identifier: String, value: AnyRef) =
    session.getGlobal(identifier) match {
      case currentValue: AnyRef => throw new IllegalArgumentException("Global already defined: (%s -> %s)".format(identifier, currentValue))
      case _ => session.setGlobal(identifier, value)
    }

  def addGlobals(globals: Map[String,AnyRef]) {
    globals.foreach { global => addGlobal(global._1, global._2) }
  }

  def clock[T <: SessionClock] = session.getSessionClock.asInstanceOf[T]
  def timerService[T <: TimerService]: TimerService = {
    val sessionClock = session.getSessionClock[SessionClock]
    if (sessionClock.isInstanceOf[TimerService])
      sessionClock.asInstanceOf[TimerService]
    else
      throw new IllegalStateException("Session's clock !isInstanceOf TimerService: clock.class=" + sessionClock.getClass)
  }

  def handleOf(fact: AnyRef): FactHandle = {
    session.getFactHandle(fact) match {
      case handle: FactHandle => handle
      case null => throw new FactException("Fact handle not found")
    }
  }

  def updateFact (fact: AnyRef) {
    session.getFactHandle(fact) match {
      case handle: FactHandle => session insert handle
      case null => throw new FactException("Fact handle not found: " + fact)
    }
  }

  def retractFact (fact: AnyRef) {
    session.getFactHandle(fact) match {
      case handle: FactHandle => session retract handle
      case null => throw new FactException("Fact handle not found: " + fact)
    }
  }

  def fire() = session.fireAllRules()
  def fire(max: Int) = session.fireAllRules(max)
  def fire(filter: AgendaFilter) = session.fireAllRules(filter)

  def facts[T: Manifest]: Set[T] = {
    val filter = new ObjectFilter() {
      def accept(obj: AnyRef) = {
        manifest[T].erasure.isAssignableFrom(obj.getClass)
      }
    }
    var results = Set[T]()
    results ++ (for (obj <- session.getObjects(filter)) yield obj)
    results
  }

  def onInserted[T](f : T => Any)(implicit m: Manifest[T]) {
    session addEventListener new DefaultWorkingMemoryEventListener {
      override def objectInserted(e: ObjectInsertedEvent) =
        if (m.erasure.isAssignableFrom(e.getObject.getClass)) {
          f(e.getObject.asInstanceOf[T])
        }
    }
  }
  def onInserted(f : ObjectInsertedEvent => Unit) {
    session addEventListener new DefaultWorkingMemoryEventListener {
      override def objectInserted(e: ObjectInsertedEvent) = f(e)
    }
  }

  def onRetracted[T](f : T => Any)(implicit m: Manifest[T]) {
    session addEventListener new DefaultWorkingMemoryEventListener {
      override def objectRetracted(e: ObjectRetractedEvent) =
        if (m.erasure.isAssignableFrom(e.getOldObject.getClass)) f(e.getOldObject.asInstanceOf[T])
    }
  }
  def onRetracted(f : ObjectRetractedEvent => Unit) {
    session addEventListener new DefaultWorkingMemoryEventListener {
      override def objectRetracted(e: ObjectRetractedEvent) = f(e)
    }
  }

  def onUpdated[T](f : T => Any)(implicit m: Manifest[T]) {
    session addEventListener new DefaultWorkingMemoryEventListener {
      override def objectUpdated(e: ObjectUpdatedEvent) =
        if (m.erasure.isAssignableFrom(e.getObject.getClass)) f(e.getObject.asInstanceOf[T])
    }
  }
  def onUpdated(f : ObjectUpdatedEvent => Unit) {
    session addEventListener new DefaultWorkingMemoryEventListener {
      override def objectUpdated(e: ObjectUpdatedEvent) = f(e)
    }
  }

  def onAfterActivationFired(f : AfterActivationFiredEvent => Unit) {
    session addEventListener new DefaultAgendaEventListener {
      override def afterActivationFired(e: AfterActivationFiredEvent) = f(e)
    }
  }
  def onBeforeActivationFired(f : BeforeActivationFiredEvent => Unit) {
    session addEventListener new DefaultAgendaEventListener {
      override def beforeActivationFired(e: BeforeActivationFiredEvent) = f(e)
    }
  }
}

class RichGlobals(globals: Globals) {

  def + (elem: (String, AnyRef)): RichGlobals = {
    globals.set(elem._1, elem._2)
    this
  }

  def + (elem1: (String, AnyRef), elem2: (String, AnyRef), elems: (String, AnyRef)*): RichGlobals =
    this + elem1 + elem2 ++ elems

  def ++(elems: Traversable[(String, AnyRef)]): RichGlobals =
    (this /: elems) (_ + _)

  def - (identifier: String): RichGlobals = {
    globals.set(identifier, null)
    this
  }

  def get(identifier: String): Option[AnyRef] =
    globals.get(identifier) match {
      case global: AnyRef => Some(global)
      case _ => None
    }

  def apply(identifier: String): Option[AnyRef] =
    globals.get(identifier) match {
      case global: AnyRef => Some(global)
      case _ => throw new NoSuchElementException("Global not found: " + identifier)
    }
}

class RichSessionPseudoClock(clock: SessionPseudoClock) {

  def currentTime = new DateTime(clock.getCurrentTime)

  def advanceTo(time: DateTime): DateTime = advanceBy(time.getMillis - clock.getCurrentTime())

  def withTime(time: DateTime): SessionPseudoClock = {
    advanceTo(time)
    clock
  }

  def advanceBy(amount: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): DateTime = {
    clock.advanceTime(amount, unit)
    //TODO This is not the right place to update the org.joda time. Should be a/the composite-clock that
    // combines all relevant clocks.
    DateTimeUtils.setCurrentMillisFixed(clock.getCurrentTime)
    new DateTime(clock.getCurrentTime)
  }
  
  //TODO When used with scala-time these two methods will conflict.
  def advanceBy(amount: Period): DateTime = advanceBy(amount.toStandardDuration.getMillis)
  //def advanceBy(amount: Duration): Unit = advanceBy(amount.getMillis)
}

