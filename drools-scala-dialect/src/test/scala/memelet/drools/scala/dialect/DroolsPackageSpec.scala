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

      Rule("rule1 v2.1") When {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} Then { (f1_1: FactOne, f2_1: FactTwo) =>
        println("**then: f1_1=%s".format(f1_1))
        println("**then: f2_1=%s".format(f2_1))
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