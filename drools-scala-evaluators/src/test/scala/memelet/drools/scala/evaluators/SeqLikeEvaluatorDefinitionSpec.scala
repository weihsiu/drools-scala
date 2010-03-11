package memelet.drools.scala.evaluators

import org.drools.base.ValueType
import org.junit.{Ignore, Test}

class SeqLikeEvaluatorDefinitionSpec extends AbstractEvaluatorDefinitionSpec(new SeqLikeEvaluatorDefinition) {

  import Evaluators._
  
  val contains = definition.ContainsOperator operatorString
  val not_contains = definition.ContainsOperator.negated operatorString
  val memberOf = definition.MemberOfOperator operatorString
  val not_memberOf = definition.MemberOfOperator.negated operatorString

  @Test def validateContains {
    val data = Seq[TestData](
      (Seq("a", "b"), contains, "a", true),
      (Seq("a", "b"), contains, "b", true),
      (Seq("a", "b"), contains, "c", false),
      (Seq.empty, contains, "c", false)
    )
    validate(ValueType.OBJECT_TYPE, data ++ not(data))
  }

  //TODO This does not yet work. Need to do something like what drools-core SetEvaluatorsDefinition is doing.
  // Somehow we need to convert Array into WrappedArray so we can use SeqLike
  @Ignore @Test def validateContainsWithArrays {
    val data = Seq[TestData](
      (Array("a", "b"), contains, "a", true),
      (Array("a", "b"), contains, "b", true),
      (Array("a", "b"), contains, "c", false),
      (Array.empty, contains, "c", false)
    )
    validate(ValueType.OBJECT_TYPE, data ++ not(data))
  }

  @Test def validateNullContains {
    val data = Seq[TestData](
      (null, contains, "c", false),
      (null, not_contains, "c", false)
    )
    validate(ValueType.OBJECT_TYPE, data)
  }

  @Test def validateMemberOf {
    val data = Seq[TestData](
      ("a", memberOf, Seq("a", "b"), true),
      ("b", memberOf, Seq("a", "b"), true),
      ("c", memberOf, Seq("a", "b"), false),
      ("c", memberOf, Seq(), false)
    )
    validate(ValueType.OBJECT_TYPE, data ++ not(data))
  }

  @Test def validateNullMemberOf {
    val data = Seq[TestData](
      (null, memberOf, Seq("c"), false),
      (null, not_memberOf, Seq("c"), false)
    )
    validate(ValueType.OBJECT_TYPE, data)
  }

}