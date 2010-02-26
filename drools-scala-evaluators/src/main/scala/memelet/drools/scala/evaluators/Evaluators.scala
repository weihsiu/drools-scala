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

//TODO
// - currently hard-coded to assume context: ObjectVariableContextEntry
// - improve the usage of 'right', 'left', 'factValue', 'otherValue' to conform better to the rete semantics
//
trait EvaluateMethods[R,L] { self: Evaluator =>

  // In Drools speak, 'Right' really means the pattern fact value on the left: Fact(value == ...)
  // 1) fact.prop == null => false                 // eg, SetHolder(set contains [x,y])
  // 2) fact.prop == null => fact.prop == value    // eg, StringHolder(str == value)
  val evalNullFactValue: Boolean

  def eval(factValue: R, value: L): Boolean

  private def isNegated = getOperator.isNegated
  private implicit def anyToR(any: Any): R = any.asInstanceOf[R]
  private implicit def anyToL(any: Any): L = any.asInstanceOf[L]

  def evaluate(workingMemory: InternalWorkingMemory, extractor: InternalReadAccessor, fact: Any, value: FieldValue): Boolean = {
    val factValue = extractor.getValue(workingMemory, fact)
    val otherValue = value.getValue
    if (factValue == null && !evalNullFactValue)
      false
    else {
      isNegated ^ eval(factValue, otherValue)
    }
  }

  def evaluateCachedRight(workingMemory: InternalWorkingMemory, context: VariableContextEntry, left: Any): Boolean = {
    if (context.rightNull && !evalNullFactValue)
      false
    else {
      val factValue = context.asInstanceOf[ObjectVariableContextEntry].right
      val otherValue = context.declaration.getExtractor.getValue(workingMemory, left)
      isNegated ^ eval(factValue, otherValue)
    }
  }

  def evaluateCachedLeft(workingMemory: InternalWorkingMemory, context: VariableContextEntry, right: Any): Boolean = {
    if (context.leftNull && !evalNullFactValue)
      false
    else {
      val factValue = context.declaration.getExtractor.getValue(workingMemory, right)
      val otherValue = context.asInstanceOf[ObjectVariableContextEntry].left
      isNegated ^ eval(factValue, otherValue)
    }
  }

  def evaluate(workingMemory: InternalWorkingMemory, leftExtractor: InternalReadAccessor, left: Any, rightExtractor: InternalReadAccessor, right: Any): Boolean = {
    val factValue: Any = leftExtractor.getValue(workingMemory, left)
    if (factValue == null & !evalNullFactValue)
      false
    else {
      val otherValue: Any = rightExtractor.getValue(workingMemory, right)
      isNegated ^ eval(factValue, otherValue)
    }
  }

}


