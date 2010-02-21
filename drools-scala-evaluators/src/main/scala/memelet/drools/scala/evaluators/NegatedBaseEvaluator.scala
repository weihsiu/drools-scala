package memelet.drools.scala.evaluators

import org.drools.common.InternalWorkingMemory
import org.drools.rule.VariableRestriction.VariableContextEntry
import org.drools.spi.Evaluator
import org.drools.spi.FieldValue
import org.drools.spi.InternalReadAccessor

trait NegatedBaseEvaluator extends Evaluator {
  abstract override def evaluate(workingMemory: InternalWorkingMemory, extractor: InternalReadAccessor, fact: Any, value: FieldValue): Boolean =
    ! super.evaluate(workingMemory, extractor, fact, value)
  abstract override def evaluateCachedRight(workingMemory: InternalWorkingMemory, context: VariableContextEntry, left: Any): Boolean =
    ! super.evaluateCachedRight(workingMemory, context, left)
  abstract override def evaluateCachedLeft(workingMemory: InternalWorkingMemory, context: VariableContextEntry, right: Any): Boolean =
    ! super.evaluateCachedLeft(workingMemory, context, right)
  abstract override def evaluate(workingMemory: InternalWorkingMemory, leftExtractor: InternalReadAccessor, left: Any, rightExtractor: InternalReadAccessor, right: Any): Boolean =
    ! super.evaluate(workingMemory, leftExtractor, left, rightExtractor, right)
}


