package memelet.drools.scala.evaluators

import org.junit.Test
import org.drools.base.ValueType

class EnumerationValueEvaluatorDefinitionSpec extends EvaluatorDefinitionSpec(new EnumerationValueEvaluatorDefinition) {

  object Enum extends Enumeration {
    val V1 = Value("V1")
    val V2 = Value("V2")
  }
  import Enum._

  @Test def validate {
    val data = Seq[TestData](
      (V1, "isNamed", "V1", true),
      (V1, "isNamed", "V2", false),
      (V2, "isNamed", "V2", true),
      (V2, "isNamed", "V1", false)
    )
    validate(ValueType.OBJECT_TYPE, data ++ not(data))
  }

  //TODO @Test def validateNulls {
  
}