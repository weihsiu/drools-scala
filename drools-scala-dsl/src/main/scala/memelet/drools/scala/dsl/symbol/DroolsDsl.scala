package memelet.drools.scala.dsl.symbol

import org.drools.reteoo.ReteooRuleBase

object DroolsDsl extends DroolsDsl

trait DroolsDsl {

  def using(rbase: ReteooRuleBase)(build: => Unit) {
    val context = RuleBaseContext.create(rbase)
    context.bindToCurrentThread
    try
      build
    finally
      context.unbindFromCurrentThread
  }

}