package memelet.drools.scala.dialect

import org.specs.mock.Mockito
import org.specs.SpecsMatchers
import memelet.drools.scala.{DroolsFixture, RichDrools}
import org.junit.Test

case class FactOne(name: String)
case class FactTwo(name: String, f: FactOne)

class ScalaDialectSpec extends SpecsMatchers with Mockito {

  @Test def scala_consequence {
    val drools = DroolsFixture(rules = Seq("memelet/drools/scala/dialect/scala_dialect.drl"))
    import drools._
    import RichDrools._

    val f1_1 = FactOne("f1_1#instance")
    val f2_1 = FactTwo("f2_1#instance", f1_1)
    session insert f1_1
    session insert f2_1
    session fireAllRules
  }

  @Test def mvel_consequence {
    val drools = DroolsFixture(rules = Seq("memelet/drools/scala/dialect/mvel_dialect.drl"))
    import drools._
    import RichDrools._

    val f1_1 = FactOne("f1_1")
    session insert f1_1
    session fireAllRules
  }


}