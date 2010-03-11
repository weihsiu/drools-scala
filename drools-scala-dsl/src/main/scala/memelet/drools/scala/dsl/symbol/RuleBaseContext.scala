package memelet.drools.scala.dsl.symbol

import org.drools.reteoo.ReteooRuleBase

trait RuleBaseContext {
  def rbase: ReteooRuleBase
  def bindToCurrentThread = RuleBaseContext.currentRBase = Some(this)
  def unbindFromCurrentThread = RuleBaseContext.currentRBase = None
}

object RuleBaseContext {

  private val threadLocal = new ThreadLocal[Option[RuleBaseContext]] {
    override def initialValue = None
  }

  def create(rbase: ReteooRuleBase) = new RuleBaseContext {
    def rbase = rbase
  }

  def currentRBase = threadLocal.get.getOrElse {
    throw new RuntimeException("No RuleBase is bound to current thread, a RuleBase must be created via RuleBaseContext.create \nand bound to the thread via 'bindToCurrentThread'")
  }

  def hasCurrentRBase = threadLocal.get != None

  private def currentRBase_=(context: Option[RuleBaseContext]) = threadLocal.set(context)
}