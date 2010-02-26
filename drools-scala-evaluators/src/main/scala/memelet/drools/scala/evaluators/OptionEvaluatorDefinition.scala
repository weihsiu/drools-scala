package memelet.drools.scala.evaluators

import org.drools.spi.Evaluator
import org.drools.base.{BaseEvaluator, ValueType}
import org.drools.base.evaluators.{Operator, EvaluatorDefinition}
import memelet.drools.scala.NoopExternalizable

class OptionEvaluatorDefinition extends EvaluatorDefinition
        with DelegatingGetEvaluatorMethods with NoopExternalizable {

  import EvaluatorDefinition.Target

  val IsSomeOperator = Operator.addOperatorToRegistry("isSome", false)
  val NotIsSomeOperator = Operator.addOperatorToRegistry("isSome", true)

  val getEvaluatorIds = Array(IsSomeOperator.getOperatorString)
  val isNegatable = true
  val getTarget = Target.FACT
  def supportsType(vtype: ValueType) = vtype == ValueType.OBJECT_TYPE

  //TODO Simplify and DRY
  def getEvaluator(vtype: ValueType, operatorId: String, isNegated: Boolean, parameterText: String, left: Target, right: Target): Evaluator = {
    (operatorId, isNegated) match {
      case IsSomeEvaluator(_) => IsSomeEvaluator
      case NotIsSomeEvaluator(_) => NotIsSomeEvaluator
      case _ => throw new IllegalArgumentException
    }
  }
  
  object IsSomeEvaluator extends IsSomeEvaluator(IsSomeOperator)
  object NotIsSomeEvaluator extends IsSomeEvaluator(NotIsSomeOperator)

  abstract class IsSomeEvaluator(operator: Operator)
          extends BaseEvaluator(ValueType.OBJECT_TYPE, operator)
          with EvaluateMethods[Option[_], Any] with EvaluatorOperationExtractor {

    val evalNullFactValue = false // An Option[_] should be None, never null
    def eval(factValue: Option[_], value: Any) = {
      value match {
        case Some(_) => factValue == value
        case _       => factValue == Some(value)
      }
    }

  }

}