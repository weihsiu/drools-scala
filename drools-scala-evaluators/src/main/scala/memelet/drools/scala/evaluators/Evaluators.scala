package memelet.drools.scala.evaluators

import org.drools.common.InternalWorkingMemory
import org.drools.spi.Evaluator
import org.drools.spi.FieldValue
import org.drools.spi.InternalReadAccessor
import org.drools.rule.VariableRestriction.{ObjectVariableContextEntry, VariableContextEntry}
import org.drools.RuntimeDroolsException
import org.drools.base.{ValueType, BaseEvaluator}
import org.drools.base.evaluators.{EvaluatorDefinition, Operator}

object Evaluators {

  import EvaluatorDefinition.Target

  class RichOperator(operator: Operator) {
    def unary_! : Operator = Operator.addOperatorToRegistry(operator.getOperatorString, !operator.isNegated)
    def negated : Operator = Operator.addOperatorToRegistry(operator.getOperatorString, !operator.isNegated)
    def operatorString: String = operator.getOperatorString
  }
  implicit def enrichOperator(operator: Operator) = new RichOperator(operator)

  //---- EvaluatorDefinition ----

  abstract class RichEvaluatorDefinition(target: Target) extends EvaluatorDefinition {

    class OperatorNotSupportedException(vtype: ValueType, operatorId: String, isNegated: Boolean, parameterText: String, left: Target, right: Target)
            extends RuntimeDroolsException(
              "Opeartor not supported: valueType=%s, operatorId=%s, isNegated=%s, params=%s, leftTarget=%s, rightTarget=%s".format(
              vtype, operatorId, isNegated, parameterText, left, right))

    def registerOperator(operatorId: String, isNegated: Boolean = false): Operator =
      Operator.addOperatorToRegistry(operatorId, isNegated)

    private[Evaluators] var evaluators = Set[Evaluator]()

    final def registerEvaluator(evaluator: Evaluator) = { evaluators += evaluator; evaluator }
    final def registerEvaluators(evaluators: Evaluator*) {
      evaluators.foreach(registerEvaluator _)
    }

    final def getTarget = target
    lazy val isNegatable: Boolean = evaluators exists (_.getOperator.isNegated)
    lazy val getEvaluatorIds = evaluators map (_.getOperator.getOperatorString) toArray
    final def supportsType(valueType: ValueType) = evaluators exists (_.getValueType == valueType)

  }

  trait DelegatingGetEvaluatorMethods { self: RichEvaluatorDefinition =>

    def getEvaluator(vtype: ValueType, operator: Operator): Evaluator =
      self.getEvaluator(vtype, operator.getOperatorString, operator.isNegated, null)

    def getEvaluator(vtype: ValueType, operator: Operator, parameterText: String): Evaluator =
      self.getEvaluator(vtype, operator.getOperatorString, operator.isNegated, parameterText)

    def getEvaluator(vtype: ValueType, operatorId: String, isNegated: Boolean, parameterText: String): Evaluator =
      self.getEvaluator(vtype, operatorId, isNegated, parameterText, getTarget, getTarget)
  }

  trait GetEvaluatorByOperatorIdAndNegated extends DelegatingGetEvaluatorMethods {
    self: RichEvaluatorDefinition =>

    def getEvaluator(vtype: ValueType, operatorId: String, isNegated: Boolean, parameterText: String, left: Target, right: Target): Evaluator = {
      evaluators find { e =>
        e.getOperator.getOperatorString == operatorId && e.getOperator.isNegated == isNegated
      } getOrElse {
        throw new OperatorNotSupportedException(vtype, operatorId, isNegated, parameterText, left, right)
      }
    }

  }

  //---- Evaluator ----

  //TODO
  // - currently hard-coded to assume context: ObjectVariableContextEntry
  // - improve the usage of 'right', 'left', 'factValue', 'otherValue' to conform better to the rete semantics
  //
  abstract class RichEvaluator[R,L](valueType: ValueType, operator: Operator) extends BaseEvaluator(valueType, operator) {

    val nullValue: AnyRef = null

    //TODO This needs a good explanation
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
      if (factValue == null && !evalNullFactValue)
        false
      else {
        val otherValue = value.getValue
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
      if (right == null && !evalNullFactValue)
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

  trait EvaluatorOperationExtractor { evaluator: BaseEvaluator =>
    def unapply(operation: (String, Boolean)): Option[Evaluator] =
      if (operation._1 == getOperator.getOperatorString && operation._2 == getOperator.isNegated) Some(evaluator) else None
  }

}