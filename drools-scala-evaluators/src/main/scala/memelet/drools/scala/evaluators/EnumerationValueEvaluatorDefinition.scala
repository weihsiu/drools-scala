package memelet.drools.scala.evaluators

import org.drools.base.BaseEvaluator
import org.drools.base.ValueType
import org.drools.common.InternalWorkingMemory
import org.drools.rule.VariableRestriction.ObjectVariableContextEntry
import org.drools.rule.VariableRestriction.VariableContextEntry
import org.drools.spi.Evaluator
import org.drools.spi.FieldValue
import org.drools.spi.InternalReadAccessor
import org.drools.base.evaluators.{Operator, EvaluatorDefinition}
import memelet.drools.scala.NoopExternalizable

class EnumerationValueEvaluatorDefinition extends EvaluatorDefinition
        with DelegatingGetEvaluatorMethods with NoopExternalizable {

  import EvaluatorDefinition.Target

  val IsNamedOperator = Operator.addOperatorToRegistry("is_named", false)
  val NotIsNamedOperator = Operator.addOperatorToRegistry("is_named", true)

  val getEvaluatorIds = Array(IsNamedOperator.getOperatorString)
  val isNegatable = true
  val getTarget = Target.FACT
  def supportsType(vtype: ValueType) = vtype == ValueType.OBJECT_TYPE

  def getEvaluator(vtype: ValueType, operatorId: String, isNegated: Boolean, parameterText: String, left: Target, right: Target): Evaluator =
    if (isNegated) NotIsNamedEvaluator else IsNamedEvaluator

  object IsNamedEvaluator extends IsNamedEvaluator(IsNamedOperator)
  object NotIsNamedEvaluator extends IsNamedEvaluator(NotIsNamedOperator) with NegatedEvaluateMethods

  class IsNamedEvaluator(operator: Operator) extends BaseEvaluator(ValueType.OBJECT_TYPE, operator) {

    def evaluate(workingMemory: InternalWorkingMemory, extractor: InternalReadAccessor, fact: Any, value: FieldValue): Boolean = {
        val factValue = extractor.getValue(workingMemory, fact)
        if (factValue == null)
          false
        else
          factValue.asInstanceOf[Enumeration#Value].toString == value.getValue
      }

    def evaluateCachedRight(workingMemory: InternalWorkingMemory, context: VariableContextEntry, left: Any): Boolean = {
      if (context.rightNull)
        false
      else {
        val leftEnum = context.declaration.getExtractor.getValue(workingMemory, left)
        val rightName = context.asInstanceOf[ObjectVariableContextEntry].right.asInstanceOf[String]
        leftEnum.toString == rightName
      }
    }

    def evaluateCachedLeft(workingMemory: InternalWorkingMemory, context: VariableContextEntry, right: Any): Boolean = {
      val rightName = context.extractor.getValue(workingMemory, right)
      if (rightName == null)
        false
      else {
        val leftEnum = context.asInstanceOf[ObjectVariableContextEntry].left.asInstanceOf[Enumeration#Value]
        leftEnum.toString == rightName
      }
    }

    def evaluate(workingMemory: InternalWorkingMemory, leftExtractor: InternalReadAccessor, left: Any, rightExtractor: InternalReadAccessor, right: Any): Boolean = {
      throw new UnsupportedOperationException("Can't figure out the left and right: debug now to verify the comments")
    }

  }

}
