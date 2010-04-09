package memelet.drools.scala

import org.drools.runtime.StatefulKnowledgeSession
import org.drools.conf.EventProcessingOption
import org.drools.runtime.conf.ClockTypeOption
import org.drools.event.rule._
import scala.collection.mutable

object DroolsFixture {

  import RichDrools._

  def apply(rules: Seq[String], globals: Map[String,AnyRef] = Map.empty, facts: Seq[AnyRef] = Seq.empty) = new DroolsFixture(rules, (session: StatefulKnowledgeSession) => {
    globals.foreach(global => session addGlobal (global._1, global._2))
    facts.foreach(fact => session insert fact)
  })

  def clock =
}

class DroolsFixture(drls: Seq[String], setup: StatefulKnowledgeSession => Unit) extends DroolsDebug {

  import RichDrools._

  val session: StatefulKnowledgeSession = {
    System.setProperty("drools.dialect.java.lngLevel", "1.6");
    System.setProperty("drools.dialect.mvel.strict", "false") //default=true
    System.setProperty("drools.dialect.mvel.langLevel", "4")  //default=4
    DroolsBuilder
            .buildKnowledgeBase(DroolsBuilder.buildKnowledgePackages(drls), EventProcessingOption.STREAM)
            .statefulSession(ClockTypeOption.get("pseudo"))
  }

  val clock = new CompositeClock(session.getSessionClock())

  var rulesFired = Set[String]()
  session.onAfterActivationFired{ e: AfterActivationFiredEvent =>
    rulesFired += e.getActivation.getRule.getName
  }

  var factEvents = Vector[WorkingMemoryEvent]()
  session addEventListener new WorkingMemoryEventListener {
    def objectInserted(e: ObjectInsertedEvent) = factEvents = factEvents.appendBack(e)
    def objectRetracted(e: ObjectRetractedEvent) = factEvents = factEvents.appendBack(e)
    def objectUpdated(e: ObjectUpdatedEvent) = factEvents = factEvents.appendBack(e)
  }

  setup(session)

  def fireRules = session.fireAllRules

  def debugWorkingMemory(): Unit = debugWorkingMemory(session)
  def debugAgenda(): Unit = debugAgenda(session: StatefulKnowledgeSession)

}

object DroolsDebug extends DroolsDebug
trait DroolsDebug {

  def log(message: String): Unit = println(message)
  
  def debugWorkingMemory(session: StatefulKnowledgeSession) {
    session addEventListener new WorkingMemoryEventListener {
      def objectInserted(e: ObjectInsertedEvent) = log("on-insert (%s)".format(e.getObject))
      def objectRetracted(e: ObjectRetractedEvent) = log("on-retract (%s)".format(e.getOldObject))
      def objectUpdated(e: ObjectUpdatedEvent) = log("on-update (%s -> %s)".format(e.getOldObject, e.getObject))
    }
  }

  def debugAgenda(session: StatefulKnowledgeSession) {
    session addEventListener new DebugAgendaEventListener {
      def p(name: String, e: ActivationEvent) = log("on-%s (%s)".format(name, e.getActivation.getRule.getName))
      def p(name: String, e: AgendaGroupEvent) = log("on-%s (%s)".format(name, e.getAgendaGroup.getName))
      override def activationCreated(e: ActivationCreatedEvent) = p("activated", e)
      override def activationCancelled(e: ActivationCancelledEvent) = p("cancelled", e)
      override def afterActivationFired(e: AfterActivationFiredEvent) = p("after", e)
      override def beforeActivationFired(e: BeforeActivationFiredEvent) = p("before", e)
      override def agendaGroupPopped(e: AgendaGroupPoppedEvent) = p("popped", e)
      override def agendaGroupPushed(e: AgendaGroupPushedEvent) = p("pushed", e)
    }
  }

}


