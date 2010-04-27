package memelet.drools.scala

trait DroolsLogging {

  import org.drools.runtime.StatefulKnowledgeSession
  import org.drools.event.rule._
  
  def log(message: String): Unit

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