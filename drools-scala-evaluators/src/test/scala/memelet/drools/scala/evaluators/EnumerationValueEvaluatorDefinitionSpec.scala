package memelet.drools.scala.evaluators

import org.specs.SpecsMatchers
import memelet.drools.scala.{DroolsFixture, RichDrools}
import org.junit.Test

class EnumerationValueEvaluatorDefinitionSpec extends SpecsMatchers {

  import RichDrools._

  object FactValue extends Enumeration {
    val E1 = Value("E1")
    val E2 = Value("E2")
  }
  import FactValue._

  case class Fact(value: FactValue.Value)

  val drools = DroolsFixture(rules = Seq("memelet/drools/scala/evaluators/enum_evaluator.drl"))
  import drools._

  @Test def enum_is_named {
    session insert Fact(E1)
    session fire

    rulesFired must_== Set("is_named E1")
  }

  @Test def enum_is_not_named {
    session insert Fact(E2)
    session fire

    rulesFired must_== Set("not is_named E1")
  }
}