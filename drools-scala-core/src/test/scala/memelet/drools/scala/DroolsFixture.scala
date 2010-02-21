package memelet.drools.scala

import org.drools.runtime.StatefulKnowledgeSession
import org.drools.conf.EventProcessingOption
import org.drools.runtime.conf.ClockTypeOption
import org.drools.event.rule._

object DroolsFixture {

  import RichDrools._

//  def apply(rules: Seq[String])(setup: StatefulKnowledgeSession => Unit) = new DroolsFixture(rules, setup)

  def apply(rules: Seq[String], globals: Map[String,AnyRef] = Map.empty, facts: Seq[AnyRef] = Seq.empty) = new DroolsFixture(rules, (session: StatefulKnowledgeSession) => {
    globals.foreach(global => session addGlobal (global._1, global._2))
    facts.foreach(fact => session insert fact)
  })

}

class DroolsFixture(drls: Seq[String], setup: StatefulKnowledgeSession => Unit) {

  import RichDrools._

  val session = {
    System.setProperty("drools.dialect.java.lngLevel", "1.6");
    System.setProperty("drools.dialect.mvel.strict", "false") //default=true
    System.setProperty("drools.dialect.mvel.langLevel", "4")  //default=4
    DroolsBuilder
            .buildKnowledgeBase(DroolsBuilder.buildKnowledgePackages(drls), EventProcessingOption.STREAM)
            .statefulSession(ClockTypeOption.get("pseudo"))
  }

  session.onAfterActivationFired{ e: AfterActivationFiredEvent =>
    rulesFired += e.getActivation.getRule.getName
  }

  val clock = new CompositeClock(session.getSessionClock())

  var rulesFired = Set[String]()

  setup(session)

  def debugWorkingMemory(session: StatefulKnowledgeSession) {
    session addEventListener new WorkingMemoryEventListener {
      def objectInserted(e: ObjectInsertedEvent) = println("on-insert (%s)".format(e.getObject.toString))
      def objectRetracted(e: ObjectRetractedEvent) = println("on-retract (%s)".format(e.getOldObject.toString))
      def objectUpdated(e: ObjectUpdatedEvent) = println("on-update (%s)".format(e.getObject.toString))
    }
  }

  def debugAgenda(session: StatefulKnowledgeSession) {
    session addEventListener new DebugAgendaEventListener {
      def p(name: String, e: ActivationEvent) = println("on-%s (%s)".format(name, e.getActivation.getRule.getName))
      def p(name: String, e: AgendaGroupEvent) = println("on-%s (%s)".format(name, e.getAgendaGroup.getName))
      override def activationCreated(e: ActivationCreatedEvent) = p("activated", e)
      override def activationCancelled(e: ActivationCancelledEvent) = p("cancelled", e)
      override def afterActivationFired(e: AfterActivationFiredEvent) = p("after", e)
      override def beforeActivationFired(e: BeforeActivationFiredEvent) = p("before", e)
      override def agendaGroupPopped(e: AgendaGroupPoppedEvent) = p("popped", e)
      override def agendaGroupPushed(e: AgendaGroupPushedEvent) = p("pushed", e)
    }
  }

}


