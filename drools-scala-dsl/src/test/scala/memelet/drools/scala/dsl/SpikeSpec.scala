package memelet.drools.scala.dsl


import org.specs.SpecsMatchers
import org.specs.mock.Mockito
import org.drools.base.{FieldFactory, ValueType}
import org.drools.spi.{Evaluator, FieldValue, InternalReadAccessor}
import org.drools.rule.Declaration
import org.drools.rule.VariableRestriction.{ObjectVariableContextEntry, VariableContextEntry}
import org.drools.base.evaluators.{EvaluatorDefinition, EvaluatorRegistry}
import org.drools.common.InternalWorkingMemory
import org.junit.Test
import memelet.drools.scala.{RichDrools, DroolsFixture}

case class FactOne(name: String)
case class FactTwo(name: String, f: FactOne)

class SpikeSpec extends SpecsMatchers with Mockito {

  val drools = DroolsFixture(rules = Seq("memelet/drools/scala/dsl/spike_spec.drl"))
  import drools._
  import RichDrools._

  @Test def spike {
    val f1_1 = FactOne("f1_1")
    val f1_2 = FactOne("f1_2")
    val f2_1 = FactTwo("f2_1", f1_1)
    session insert f1_1
    session insert f1_2
    session insert f2_1
    session fireAllRules
  }
  
}
