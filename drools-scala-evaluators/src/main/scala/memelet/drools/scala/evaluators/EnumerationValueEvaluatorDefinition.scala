package memelet.drools.scala.evaluators

import org.drools.spi.Evaluator
import org.drools.base.{BaseEvaluator, ValueType}
import org.drools.base.evaluators.{Operator, EvaluatorDefinition}
import memelet.drools.scala.NoopExternalizable

class EnumerationValueEvaluatorDefinition extends EvaluatorDefinition
        with DelegatingGetEvaluatorMethods with NoopExternalizable {

  import EvaluatorDefinition.Target

  val IsNamedOperator = Operator.addOperatorToRegistry("isNamed", false)
  val NotIsNamedOperator = Operator.addOperatorToRegistry("isNamed", true)

  val getEvaluatorIds = Array(IsNamedOperator.getOperatorString)
  val isNegatable = true
  val getTarget = Target.FACT
  def supportsType(vtype: ValueType) = vtype == ValueType.OBJECT_TYPE

  def getEvaluator(vtype: ValueType, operatorId: String, isNegated: Boolean, parameterText: String, left: Target, right: Target): Evaluator = {
    (operatorId, isNegated) match {
      case IsNamedEvaluator(_) => IsNamedEvaluator
      case NotIsNamedEvaluator(_) => NotIsNamedEvaluator
      case _ => throw new IllegalArgumentException
    }
  }
//
//  def getEvaluator(vtype: ValueType, operatorId: String, isNegated: Boolean, parameterText: String, left: Target, right: Target): Evaluator =
//    if (isNegated) NotIsNamedEvaluator else IsNamedEvaluator

  object IsNamedEvaluator extends IsNamedEvaluator(IsNamedOperator)
  object NotIsNamedEvaluator extends IsNamedEvaluator(NotIsNamedOperator)

  abstract class IsNamedEvaluator(operator: Operator)
          extends BaseEvaluator(ValueType.OBJECT_TYPE, operator)
          with EvaluateMethods[Enumeration#Value, String] with EvaluatorOperationExtractor {

    val evalNullFactValue = true
    def eval(factValue: Enumeration#Value, value: String) = factValue.toString == value

  }
  
}
