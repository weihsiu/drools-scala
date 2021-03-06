package memelet.drools.scala.evaluators

import org.junit.Test
import org.drools.base.ValueType

class OptionEvaluatorDefinitionSpec extends AbstractEvaluatorDefinitionSpec(new OptionEvaluatorDefinition) {

  @Test def validate {
    val data = Seq[TestData](
      (Some("some"), "isSome", "some", true),
      (Some("some"), "isSome", "other", false),
      (Some("other"), "isSome", "some", false),
      (Some("some"), "isSome", Some("some"), true),
      (Some("some"), "isSome", Some("other"), false),
      (Some("other"), "isSome", Some("some"), false),
      (Some("some"), "isSome", None, false),
      (None, "isSome", Some("some"), false),
      (None, "isSome", None, true)
    )
    validate(ValueType.OBJECT_TYPE, data ++ not(data))
  }

  //TODO @Test def validateNulls {

}