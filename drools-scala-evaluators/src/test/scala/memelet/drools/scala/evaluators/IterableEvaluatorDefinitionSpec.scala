package memelet.drools.scala.evaluators

import org.drools.base.ValueType
import org.junit.{Ignore, Test}

class IterableEvaluatorDefinitionSpec extends AbstractEvaluatorDefinitionSpec(new IterableEvaluatorDefinition) {

  import Evaluators._
  
  val contains = definition.ContainsOperator operatorString
  val not_contains = definition.ContainsOperator.negated operatorString
  val memberOf = definition.MemberOfOperator operatorString
  val not_memberOf = definition.MemberOfOperator.negated operatorString

  @Test def validateContainsSeqLike {
    val data = Seq[TestData](
      (Seq("a", "b"), contains, "a", true),
      (Seq("a", "b"), contains, "b", true),
      (Seq("a", "b"), contains, "c", false),
      (Seq.empty, contains, "c", false)
    )
    validate(ValueType.OBJECT_TYPE, data ++ not(data))
  }

  @Test def validateContainsSetLike {
    val data = Seq[TestData](
      (Set("a", "b"), contains, "a", true),
      (Set("a", "b"), contains, "b", true),
      (Set("a", "b"), contains, "c", false),
      (Set.empty, contains, "c", false)
    )
    validate(ValueType.OBJECT_TYPE, data ++ not(data))
  }

  @Test def validateContainsArrayLike {
    val data = Seq[TestData](
      (Array("a", "b"), contains, "a", true),
      (Array("a", "b"), contains, "b", true),
      (Array("a", "b"), contains, "c", false),
      (Array.empty, contains, "c", false)
    )
    validate(ValueType.ARRAY_TYPE, data ++ not(data))
  }

  @Test def validateNullContains {
    val data = Seq[TestData](
      (null, contains, "c", false),
      (null, not_contains, "c", false)
    )
    validate(ValueType.OBJECT_TYPE, data)
    validate(ValueType.ARRAY_TYPE, data)
  }

  @Test def validateMemberOfSeqLike {
    val data = Seq[TestData](
      ("a", memberOf, Seq("a", "b"), true),
      ("b", memberOf, Seq("a", "b"), true),
      ("c", memberOf, Seq("a", "b"), false),
      ("c", memberOf, Seq(), false)
    )
    validate(ValueType.OBJECT_TYPE, data ++ not(data))
  }

  @Test def validateMemberOfSetLike {
    val data = Seq[TestData](
      ("a", memberOf, Set("a", "b"), true),
      ("b", memberOf, Set("a", "b"), true),
      ("c", memberOf, Set("a", "b"), false),
      ("c", memberOf, Set(), false)
    )
    validate(ValueType.OBJECT_TYPE, data ++ not(data))
  }

  @Test def validateMemberOfArrayLike {
    val data = Seq[TestData](
      ("a", memberOf, Array("a", "b"), true),
      ("b", memberOf, Array("a", "b"), true),
      ("c", memberOf, Array("a", "b"), false),
      ("c", memberOf, Array(), false)
    )
    validate(ValueType.ARRAY_TYPE, data ++ not(data))
  }

  @Test def validateNullMemberOfSeqLike {
    val data = Seq[TestData](
      (null, memberOf, Seq("c"), false),
      (null, not_memberOf, Seq("c"), false)
    )
    validate(ValueType.OBJECT_TYPE, data)
  }

  @Test def validateNullMemberOfSetLike {
    val data = Seq[TestData](
      (null, memberOf, Set("c"), false),
      (null, not_memberOf, Set("c"), false)
    )
    validate(ValueType.OBJECT_TYPE, data)
  }

  @Test def validateNullMemberOfArrayLike {
    val data = Seq[TestData](
      (null, memberOf, Array("c"), false),
      (null, not_memberOf, Array("c"), false)
    )
    validate(ValueType.ARRAY_TYPE, data)
  }

}