package memelet.drools.scala.evaluators

import org.drools.base.{BaseEvaluator, ValueType}
import org.drools.base.evaluators.{Operator, EvaluatorDefinition}
import EvaluatorDefinition.Target
import memelet.drools.scala.NoopExternalizable

class OptionEvaluatorDefinition extends RichEvaluatorDefinition(Target.FACT)
        with GetEvaluatorByOperatorIdAndNegated
        with NoopExternalizable {

  val IsSomeOperator = registerOperator("isSome")
  registerEvaluator(new IsSomeEvaluator(IsSomeOperator))
  registerEvaluator(new IsSomeEvaluator(IsSomeOperator negated))

  private class IsSomeEvaluator(operator: Operator) extends BaseEvaluator(ValueType.OBJECT_TYPE, operator)
          with EvaluateMethods[Option[_], Any]
          with EvaluatorOperationExtractor {

    val evalNullFactValue = false // An Option[_] should be None, never null

    def eval(factValue: Option[_], value: Any) = value match {
      case Some(_) => factValue == value
      case _       => factValue == Some(value)
    }

  }

}