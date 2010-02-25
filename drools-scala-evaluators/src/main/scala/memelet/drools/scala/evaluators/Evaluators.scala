package memelet.drools.scala.evaluators

import org.drools.common.InternalWorkingMemory
import org.drools.spi.Evaluator
import org.drools.spi.FieldValue
import org.drools.spi.InternalReadAccessor
import org.drools.base.BaseEvaluator
import org.drools.rule.VariableRestriction.{ObjectVariableContextEntry, VariableContextEntry}

trait NegatedEvaluateMethods extends Evaluator {
  abstract override def evaluate(workingMemory: InternalWorkingMemory, extractor: InternalReadAccessor, fact: Any, value: FieldValue): Boolean =
    ! super.evaluate(workingMemory, extractor, fact, value)
  abstract override def evaluateCachedRight(workingMemory: InternalWorkingMemory, context: VariableContextEntry, left: Any): Boolean =
    ! super.evaluateCachedRight(workingMemory, context, left)
  abstract override def evaluateCachedLeft(workingMemory: InternalWorkingMemory, context: VariableContextEntry, right: Any): Boolean =
    ! super.evaluateCachedLeft(workingMemory, context, right)
  abstract override def evaluate(workingMemory: InternalWorkingMemory, leftExtractor: InternalReadAccessor, left: Any, rightExtractor: InternalReadAccessor, right: Any): Boolean =
    ! super.evaluate(workingMemory, leftExtractor, left, rightExtractor, right)
}

trait EvaluatorOperationExtractor { evaluator: BaseEvaluator =>
  def unapply(operation: (String, Boolean)): Option[Evaluator] =
    if (operation._1 == getOperator.getOperatorString && operation._2 == getOperator.isNegated) Some(evaluator) else None
}

trait EvaluateMethods[R,L] { self: Evaluator =>

  protected def eval(factValue: R, value: L): Boolean

  // null == null => true
  def evaluate(workingMemory: InternalWorkingMemory, extractor: InternalReadAccessor, fact: Any, value: FieldValue): Boolean = {
    val factValue = extractor.getValue(workingMemory, fact)
    if (factValue == null)
      value.getValue == null
    else
      getOperator.isNegated ^ eval(factValue.asInstanceOf[R], value.getValue.asInstanceOf[L])
  }

  // null == null => true
  def evaluateCachedRight(workingMemory: InternalWorkingMemory, context: VariableContextEntry, left: Any): Boolean = {
    if (context.rightNull)
      left == null
    else {
      val factValue = context.asInstanceOf[ObjectVariableContextEntry].right
      val value = context.declaration.getExtractor.getValue(workingMemory, left)
      getOperator.isNegated ^ eval(factValue.asInstanceOf[R], value.asInstanceOf[L])
    }
  }

  // null == null => true
  def evaluateCachedLeft(workingMemory: InternalWorkingMemory, context: VariableContextEntry, right: Any): Boolean = {
    if (context.leftNull)
      right == null
    else {
      val factValue = context.declaration.getExtractor.getValue(workingMemory, right)
      val value = context.asInstanceOf[ObjectVariableContextEntry].left
      getOperator.isNegated ^ eval(factValue.asInstanceOf[R], value.asInstanceOf[L])
    }
  }

  def evaluate(workingMemory: InternalWorkingMemory, leftExtractor: InternalReadAccessor, left: Any, rightExtractor: InternalReadAccessor, right: Any): Boolean = {
    throw new UnsupportedOperationException("Can't figure out the left and right: debug now to verify the comments")
//      val optionValue: Option[_] = leftExtractor.getValue(workingMemory, left).asInstanceOf[Option[_]]
//      return optionValue.isDefined
  }

}


