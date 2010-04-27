package memelet.drools.scala

import org.drools.conf.EventProcessingOption
import org.drools.runtime.conf.ClockTypeOption
import org.drools.event.rule._
import org.drools.runtime.{ObjectFilter, StatefulKnowledgeSession}

case class DroolsFixture(rules: Seq[String], globals: Map[String,AnyRef] = Map.empty, facts: Seq[AnyRef] = Seq.empty,
        debug: Boolean = false) {

  import RichDrools._

  implicit val session: StatefulKnowledgeSession = {
    System.setProperty("drools.dialect.java.lngLevel", "1.6");
    System.setProperty("drools.dialect.mvel.strict", "false") //default=true
    System.setProperty("drools.dialect.mvel.langLevel", "4")  //default=4
    DroolsBuilder
            .buildKnowledgeBase(DroolsBuilder.buildKnowledgePackages(rules), EventProcessingOption.STREAM)
            .statefulSession(ClockTypeOption.get("pseudo"))
  }

  if (debug) {
    debugWorkingMemory
    debugAgenda
  }

  globals.foreach(global => session addGlobal (global._1, global._2))
  facts.foreach(fact => session insert fact)

  val clock = new CompositeClock(session.getSessionClock())

  var rulesFired = List[String]()
  session.onAfterActivationFired{ e: AfterActivationFiredEvent =>
    rulesFired = e.getActivation.getRule.getName :: rulesFired
  }

  var factEvents = Vector[WorkingMemoryEvent]()
  session addEventListener new WorkingMemoryEventListener {
    def objectInserted(e: ObjectInsertedEvent) = factEvents = factEvents.appendBack(e)
    def objectRetracted(e: ObjectRetractedEvent) = factEvents = factEvents.appendBack(e)
    def objectUpdated(e: ObjectUpdatedEvent) = factEvents = factEvents.appendBack(e)
  }

  def events[T: Manifest]: Seq[T] = {
    Seq.empty[T] ++ (for {
      event <- factEvents
      if (manifest[T].erasure.isAssignableFrom(event.getClass))
    } yield event.asInstanceOf[T])
  }

  def fireRules = session.fireAllRules

  def debugWorkingMemory(): Unit = DroolsDebug.debugWorkingMemory(session)
  def debugAgenda(): Unit = DroolsDebug.debugAgenda(session)

}

object DroolsDebug extends DroolsLogging {
  def log(message: String): Unit = println(message)
}


