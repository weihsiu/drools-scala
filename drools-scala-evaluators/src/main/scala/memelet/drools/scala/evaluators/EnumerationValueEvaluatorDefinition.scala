package memelet.drools.scala.evaluators

import org.drools.base.ValueType
import org.drools.base.evaluators.{Operator, EvaluatorDefinition}
import EvaluatorDefinition.Target
import memelet.drools.scala.NoopExternalizable
import memelet.drools.scala.evaluators.Evaluators._

class EnumerationValueEvaluatorDefinition extends RichEvaluatorDefinition(Target.FACT)
        with NoopExternalizable {

  val IsNamedOperator = registerOperator("isNamed")

  registerEvaluators(
    new IsNamedEvaluator(IsNamedOperator),
    new IsNamedEvaluator(IsNamedOperator negated))

  private class IsNamedEvaluator(operator: Operator) extends RichEvaluator[Enumeration#Value, String](ValueType.OBJECT_TYPE, operator)
          with EvaluatorOperationExtractor {

    lazy val evalNullFactValue = false
    
    def eval(factValue: Enumeration#Value, value: String) = factValue.toString == value
  }
  
}
