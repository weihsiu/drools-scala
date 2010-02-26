package memelet.drools.scala.evaluators

import org.drools.base.{BaseEvaluator, ValueType}
import org.drools.base.evaluators.{Operator, EvaluatorDefinition}
import EvaluatorDefinition.Target
import memelet.drools.scala.NoopExternalizable

class EnumerationValueEvaluatorDefinition extends RichEvaluatorDefinition(Target.FACT)
        with GetEvaluatorByOperatorIdAndNegated
        with NoopExternalizable {

  val IsNamedOperator = registerOperator("isNamed")
  registerEvaluator(new IsNamedEvaluator(IsNamedOperator))
  registerEvaluator(new IsNamedEvaluator(IsNamedOperator negated))

  private class IsNamedEvaluator(operator: Operator)
          extends BaseEvaluator(ValueType.OBJECT_TYPE, operator)
          with EvaluateMethods[Enumeration#Value, String] with EvaluatorOperationExtractor {

    val evalNullFactValue = true
    
    def eval(factValue: Enumeration#Value, value: String) = factValue.toString == value

  }
  
}
