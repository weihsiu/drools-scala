package memelet.drools.scala.evaluators

import org.specs.SpecsMatchers
import memelet.drools.scala.{DroolsFixture, RichDrools}
import org.junit.Test

class OptionEvaluatorDefinitionSpec extends SpecsMatchers {

  import RichDrools._

  case class Data (value: String)
  case class DataFact(value: Option[Data])(implicit val rule: String)
  case class StringFact(value: Option[String])(implicit val rule: String)

  val drools = DroolsFixture(rules = Seq("memelet/drools/scala/evaluators/OptionEvaluatorDefinitionSpec.drl"))
  import drools._

  @Test def option_is_none {
    implicit val rule = "is_none"
    session insert StringFact(None)
    session fire

    rulesFired must_== Set(rule)
  }

  @Test def option_is_not_none {
    implicit val rule = "is_not_none"
    session insert StringFact(Some("not none"))
    session fire

    rulesFired must_== Set(rule)
  }

  @Test def option_is_some_literal {
    implicit val rule = "is_some__literal"
    session insert StringFact(Some("some"))
    session fire

    rulesFired must_== Set(rule)
  }

  @Test def option_not_is_some_literal {
    implicit val rule = "not_is_some__literal"
    session insert StringFact(Some("some"))
    session fire

    rulesFired must_== Set(rule)
  }

  @Test def smoke_1 {
    implicit val rule = "smoke_1"
    session insert Data("some")
    session insert DataFact(Some(Data("some")))
    session fire

    rulesFired must_== Set(rule)
  }

  @Test def smoke_2 {
    implicit val rule = "smoke_2"
    session insert StringFact(Some("fact"))
    session fire

    rulesFired must_== Set(rule)
  }


}