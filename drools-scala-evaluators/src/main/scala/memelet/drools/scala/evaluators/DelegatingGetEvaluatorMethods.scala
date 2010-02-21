package memelet.drools.scala.evaluators

import org.drools.base.ValueType
import org.drools.spi.Evaluator
import org.drools.base.evaluators.{Operator, EvaluatorDefinition}

trait DelegatingGetEvaluatorMethods { self: EvaluatorDefinition =>

  def getEvaluator(vtype: ValueType, operator: Operator): Evaluator =
    self.getEvaluator(vtype, operator.getOperatorString, operator.isNegated, null)

  def getEvaluator(vtype: ValueType, operator: Operator, parameterText: String): Evaluator =
    self.getEvaluator(vtype, operator.getOperatorString, operator.isNegated, parameterText)

  def getEvaluator(vtype: ValueType, operatorId: String, isNegated: Boolean, parameterText: String): Evaluator =
    self.getEvaluator(vtype, operatorId, isNegated, parameterText, getTarget, getTarget)
}

