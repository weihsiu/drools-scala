package memelet.drools.scala.evaluators

import org.specs.SpecsMatchers
import org.specs.mock.Mockito
import org.drools.base.{FieldFactory, ValueType}
import org.drools.spi.{Evaluator, FieldValue, InternalReadAccessor}
import org.drools.rule.Declaration
import org.drools.rule.VariableRestriction.{ObjectVariableContextEntry, VariableContextEntry}
import org.drools.base.evaluators.{EvaluatorDefinition, EvaluatorRegistry}
import org.drools.common.InternalWorkingMemory

abstract class EvaluatorDefinitionSpec(definitionUnderTest: EvaluatorDefinition) extends SpecsMatchers with Mockito {

  val registry = new EvaluatorRegistry
  registry.addEvaluatorDefinition(definitionUnderTest)

  val notPrefix = "not "

  case class TestData(left: AnyRef, operator: String, right: AnyRef, expected: Boolean) {
    val isNegated = operator startsWith notPrefix
    val operatorId = operator stripPrefix notPrefix
  }

  def not(data: Seq[TestData]) = data.map { data => TestData(data.left, notPrefix + data.operator, data.right, !data.expected) }

  type TestTuple = (AnyRef, String, AnyRef, Boolean)

  implicit def testTupleToTestData(td: TestTuple): TestData = TestData(td._1, td._2, td._3, td._4)

  val workingMemory = mock[InternalWorkingMemory]
  val extractor = mock[InternalReadAccessor]
  extractor.getValue(any[InternalWorkingMemory], any[Object]) answers { _.asInstanceOf[Array[Object]](1) }

  def validate(valueType: ValueType, datum: Seq[TestData]) {
    for (data <- datum) {
      val evaluatorDef = registry.getEvaluatorDefinition(data.operatorId)
      val evaluator = evaluatorDef.getEvaluator(valueType, data.operatorId, data.isNegated, null)

      evaluator.getValueType must_== valueType
      evaluator.getOperator.getOperatorString must_== data.operatorId
      evaluator.getOperator.isNegated must_== data.isNegated
      
      checkEvaluatorMethodWithFieldValue(valueType, extractor, data, evaluator)
      checkEvaluatorMethodCachedRight(valueType, extractor, data, evaluator)
      checkEvaluatorMethodCachedLeft(valueType, extractor, data, evaluator)
      checkEvaluatorMethodWith2Extractors(valueType, extractor, data, evaluator)
    }
  }

  private def message(method: String, valueType: ValueType, data: TestData) = "%s for [%s %s %s]".format(method, data.left, data.operator, data.right)
  private implicit def anyToFieldValue(any: Any): FieldValue = FieldFactory.getFieldValue(any)

  private def checkEvaluatorMethodWithFieldValue(valueType: ValueType, extractor: InternalReadAccessor, data: TestData, evaluator: Evaluator) {
    evaluator.evaluate(workingMemory, extractor, data.left, data.right) aka message("FieldValue", valueType, data) must_== data.expected
  }

  private def checkEvaluatorMethodCachedRight(valueType: ValueType, extractor: InternalReadAccessor, data: TestData, evaluator: Evaluator) {
    val context = Entry(evaluator, extractor, valueType, data)
    evaluator.evaluateCachedRight(workingMemory, context, data.right) aka message("CachedRight", valueType, data) must_== data.expected
  }

  private def checkEvaluatorMethodCachedLeft(valueType: ValueType, extractor: InternalReadAccessor, data: TestData, evaluator: Evaluator) {
    val context = Entry(evaluator, extractor, valueType, data)
    evaluator.evaluateCachedLeft(workingMemory, context, data.left) aka message("CachedLeft", valueType, data) must_== data.expected
  }

  private def checkEvaluatorMethodWith2Extractors(valueType: ValueType, extractor: InternalReadAccessor, data: TestData, evaluator: Evaluator) {
    evaluator.evaluate(workingMemory, extractor, data.left, extractor, data.right) aka message("2 extractors", valueType, data) must_== data.expected
  }

  private def Entry(evaluator: Evaluator, extractor: InternalReadAccessor, valueType: ValueType, data: TestData): VariableContextEntry = {
    def throwUnsupportedType = throw new UnsupportedOperationException("extend as per org.drools.base.EvaluatorFactoryTest")
    val declaration = new Declaration("test", extractor, null)

    val coerced: ValueType = evaluator.getCoercedValueType
    if (coerced.isIntegerNumber)
      throwUnsupportedType
    else if (coerced.isChar)
      throwUnsupportedType
    else if (coerced.isBoolean)
      throwUnsupportedType
    else if (coerced.isFloatNumber)
      throwUnsupportedType

    else {
      val context: ObjectVariableContextEntry = new ObjectVariableContextEntry(extractor, declaration, evaluator)

      if (data.right == null)
        context.leftNull = true
      else
        context.left = data.right
      if (data.left == null)
        context.rightNull = true
      else
        context.right = data.left

      return context
    }
  }


}