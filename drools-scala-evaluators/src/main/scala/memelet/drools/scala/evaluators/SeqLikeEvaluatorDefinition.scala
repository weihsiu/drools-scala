package memelet.drools.scala.evaluators

import org.drools.base.ValueType
import org.drools.base.evaluators.{Operator, EvaluatorDefinition}
import EvaluatorDefinition.Target
import memelet.drools.scala.NoopExternalizable
import memelet.drools.scala.evaluators.Evaluators._
import collection.SeqLike

class SeqLikeEvaluatorDefinition extends RichEvaluatorDefinition(Target.FACT)
        with GetEvaluatorByOperatorIdAndNegated
        with NoopExternalizable {

  val ContainsOperator = registerOperator("contains")
  val MemberOfOperator = registerOperator("memberOf")

  registerEvaluators(
    new SeqLikeContainsEvaluator(ContainsOperator),
    new SeqLikeContainsEvaluator(ContainsOperator negated),
    new SeqLikeMemberOfEvaluator(MemberOfOperator),
    new SeqLikeMemberOfEvaluator(MemberOfOperator negated))

  private class SeqLikeContainsEvaluator(operator: Operator) extends RichEvaluator[SeqLike[_,_], Any](ValueType.OBJECT_TYPE, operator)
          with EvaluatorOperationExtractor {

    val evalNullFactValue = false

    def eval(factValue: SeqLike[_,_], value: Any) = factValue.contains(value)
  }

  private class SeqLikeMemberOfEvaluator(operator: Operator) extends RichEvaluator[Any, SeqLike[_,_]](ValueType.OBJECT_TYPE, operator)
          with EvaluatorOperationExtractor {

    val evalNullFactValue = true

    def eval(factValue: Any, value: SeqLike[_,_]) = value.contains(factValue)
  }
}