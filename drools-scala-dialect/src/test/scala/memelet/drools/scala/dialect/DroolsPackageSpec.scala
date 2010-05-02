package memelet.drools.scala.dialect

import org.specs.SpecsMatchers
import org.specs.mock.Mockito
import org.junit.Test
import memelet.drools.scala.DroolsBuilder
import org.drools.runtime.conf.ClockTypeOption
import org.drools.conf.EventProcessingOption
import org.drools.spi.KnowledgeHelper
import scala.collection.JavaConversions._

class DroolsPackageSpec extends SpecsMatchers with Mockito {

  @Test
  def smoke {
    
    implicit val builder = new KnowledgePackageBuilder

    new Package {

      Import[FactOne]
      Import[FactTwo]

      Rule("rule1 typed") When {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} Then { (f1_1: FactOne, f2_1: FactTwo, kh: KnowledgeHelper) =>
        println("**typed: f1_1=%s".format(f1_1))
        println("**typed: f2_1=%s".format(f2_1))
        println("**typed: kh=%s".format(kh))
      }

    }

    val session = DroolsBuilder
              .buildKnowledgeBase(builder.knowledgePackages, EventProcessingOption.STREAM)
              .statefulSession(ClockTypeOption.get("pseudo"))

    val f1_1 = FactOne("f1_1#instance")
    val f2_1 = FactTwo("f2_1#instance", f1_1)
    session insert f1_1
    session insert f2_1
    session insert FactThree("f3_1", f1_1)
    session fireAllRules
    
  }
}