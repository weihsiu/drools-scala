package memelet.drools.scala.evaluators

import org.drools.base.ValueType
import org.drools.base.evaluators.{Operator, EvaluatorDefinition}
import EvaluatorDefinition.Target
import memelet.drools.scala.NoopExternalizable
import memelet.drools.scala.evaluators.Evaluators._
import collection.{MapLike, SetLike, SeqLike}
import collection.mutable.ArrayLike

class IterableEvaluatorDefinition extends RichEvaluatorDefinition(Target.FACT)
        with NoopExternalizable {

  val ContainsOperator = registerOperator("contains")
  val MemberOfOperator = registerOperator("memberOf")

  registerEvaluators(
    new IterableContainsEvaluator(ContainsOperator),
    new IterableContainsEvaluator(ContainsOperator negated),
    new ArrayContainsEvaluator(ContainsOperator),
    new ArrayContainsEvaluator(ContainsOperator negated))
  registerEvaluators(
    new IterableMemberOfEvaluator(MemberOfOperator),
    new IterableMemberOfEvaluator(MemberOfOperator negated),
    new ArrayMemberOfEvaluator(MemberOfOperator),
    new ArrayMemberOfEvaluator(MemberOfOperator negated))

  private class IterableContainsEvaluator(operator: Operator) extends RichEvaluator[Iterable[_], Any](ValueType.OBJECT_TYPE, operator)
          with EvaluatorOperationExtractor {

    val evalNullFactValue = false

    def eval(factValue: Iterable[Any], value: Any) = factValue match {
      case seqLike: SeqLike[_,_] => seqLike.contains(value)
      case setLike: SetLike[_,_] => setLike.contains(value)
      case _ => throw new IllegalArgumentException("Unsupported Iterable '%s' for operater '%s'".format(factValue.getClass.getName, operator.getOperatorString))
    }
  }

  private class IterableMemberOfEvaluator(operator: Operator) extends RichEvaluator[Any, Iterable[_]](ValueType.OBJECT_TYPE, operator)
          with SupportsAnyObjectValueType
          with EvaluatorOperationExtractor {

    val evalNullFactValue = true

    def eval(factValue: Any, value: Iterable[Any]) = value match {
      case seqLikeValue: SeqLike[_,_] => seqLikeValue.contains(factValue)
      case setLikeValue: SetLike[_,_] => setLikeValue.contains(factValue)
      case _ => throw new IllegalArgumentException("Unsupported Iterable '%s' for operater '%s'".format(value.getClass.getName, operator.getOperatorString))
    }
  }

  private class ArrayContainsEvaluator(operator: Operator) extends RichEvaluator[Array[Any], Any](ValueType.ARRAY_TYPE, operator)
          with EvaluatorOperationExtractor {

    val evalNullFactValue = false

    def eval(factValue: Array[Any], value: Any) = factValue.contains(value)
  }

  private class ArrayMemberOfEvaluator(operator: Operator) extends RichEvaluator[Any, Array[Any]](ValueType.ARRAY_TYPE, operator)
          with EvaluatorOperationExtractor {

    val evalNullFactValue = true

    def eval(factValue: Any, value: Array[Any]) = value.contains(factValue)
  }

}