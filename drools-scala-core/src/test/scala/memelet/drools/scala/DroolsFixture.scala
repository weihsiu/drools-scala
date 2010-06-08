package memelet.drools.scala

import org.drools.conf.EventProcessingOption
import org.drools.runtime.conf.ClockTypeOption
import org.drools.event.rule._
import org.drools.runtime.{ObjectFilter, StatefulKnowledgeSession}
import org.drools.time.SessionPseudoClock
import java.util.concurrent.TimeUnit
import org.drools.definition.KnowledgePackage

case class DroolsFixture(drls: Seq[String] = Seq.empty, knowledgePackages: Seq[KnowledgePackage] = Seq.empty, 
                         globals: Map[String,AnyRef] = Map.empty, facts: Seq[AnyRef] = Seq.empty,
        debug: Boolean = false) {

  import RichDrools._

  implicit val session: StatefulKnowledgeSession = {
    System.setProperty("drools.dialect.java.langLevel", "1.6");
    System.setProperty("drools.dialect.mvel.strict", "false") //default=true
    System.setProperty("drools.dialect.mvel.langLevel", "4")  //default=4
    import DroolsBuilder._
    newKnowledgeBase(buildFileDrls(drls) ++ knowledgePackages, Seq(EventProcessingOption.STREAM))
      .newScalaStatefulKnowledgeSession(Seq(ClockTypeOption.get("pseudo")))
  }

  if (debug) {
    debugWorkingMemory
    debugAgenda
  }

  globals foreach (global => session addGlobal (global._1, global._2))
  facts foreach (fact => session insert fact)

  val sessionClock = session.getSessionClock.asInstanceOf[SessionPseudoClock]
  def advanceTimeMillis(millis: Long) = sessionClock.advanceTime(millis, TimeUnit.MILLISECONDS)
  
  var rulesFired = List[String]()
  session.onAfterActivationFired{ e: AfterActivationFiredEvent =>
    rulesFired = e.getActivation.getRule.getName :: rulesFired
  }

  var factEvents = Vector[WorkingMemoryEvent]()
  session addEventListener new WorkingMemoryEventListener {
    def objectInserted(e: ObjectInsertedEvent) = factEvents = factEvents :+ e
    def objectRetracted(e: ObjectRetractedEvent) = factEvents = factEvents :+ e
    def objectUpdated(e: ObjectUpdatedEvent) = factEvents = factEvents :+ e
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


