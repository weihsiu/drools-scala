package memelet.drools.scala.dialect

import org.specs.SpecsMatchers
import org.specs.mock.Mockito
import org.junit.Test

class DroolsPackageSpec extends SpecsMatchers with Mockito {

  @Test
  def smoke {
    
    implicit val builder = new KnowledgeBaseBuilder

    new Package {

      Import[FactOne]
      Import[FactTwo]

      rule("rule1 v2.1") when {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} then { () =>
        println("**then**")
        println("**then**")
      }

    }

    val session = builder.statefulSession

    val f1_1 = FactOne("f1_1#instance")
    val f2_1 = FactTwo("f2_1#instance", f1_1)
    session insert f1_1
    session insert f2_1
    session fireAllRules
    
  }
}