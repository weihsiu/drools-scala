package memelet.drools.scala.dialect

import org.specs.SpecsMatchers
import org.specs.mock.Mockito
import org.junit.Test
import org.drools.runtime.conf.ClockTypeOption
import org.drools.conf.EventProcessingOption
import org.drools.spi.KnowledgeHelper
import scala.collection.JavaConversions._
import memelet.drools.scala.{RichDrools, DroolsFixture, DroolsBuilder}

case class FactOne(name: String)
case class FactTwo(name: String, f: FactOne)

case class FactThree(name: String, f: FactOne)
class FactOneSub(name: String) extends FactOne(name)

class DroolsPackageSpec extends SpecsMatchers with Mockito {

  //@Test sunny day
  //@Test args out of order
  //@Test wrong number of args
  //@Test wrong args name
  //@Test wrong args type
  //@Test args supertype
  //@Test args subtype
  //@Test args special types w/any name(kh, wm)
  //@Test duplicate rule name
  //@Test explicit package name
  //@Test default package name
  //@Test explicit rule name
  //@Test generated rule name
  //@Test multiple package instances different names
  //@Test multiple package instances same names
  //@Test rule attributes (eg, salience)

  @Test
  def smoke {
    
    implicit val builder = new KnowledgePackageBuilder

    new Package {

      Import[FactOne]
      Import[FactTwo]

      Rule("rule1 typed p1.1") When {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} Then { (f1_1: FactOne, f2_1: FactTwo, kh: KnowledgeHelper) =>
        println("**typed p1.1: f1_1=%s".format(f1_1))
        println("**typed p1.1: f2_1=%s".format(f2_1))
        println("**typed p1.1: kh=%s".format(kh))
      }

      Rule("rule1 typed p1.2") When {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} Then { (f1_1: FactOne, f2_1: FactTwo, kh: KnowledgeHelper) =>
        println("**typed p1.2: f1_1=%s".format(f1_1))
        println("**typed p1.2: f2_1=%s".format(f2_1))
        println("**typed p1.2: kh=%s".format(kh))
      }

    }

    new Package {

      Import[FactOne]
      Import[FactTwo]

      Rule("rule1 typed p2") When {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} Then { (f1_1: FactOne, f2_1: FactTwo, kh: KnowledgeHelper) =>
        println("**typed 2: f1_1=%s".format(f1_1))
        println("**typed 2: f2_1=%s".format(f2_1))
        println("**typed 2: kh=%s".format(kh))
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

  @Test def mvel_consequence {
    val drools = DroolsFixture(rules = Seq("memelet/drools/scala/dialect/mvel_dialect.drl"))
    import drools._
    import RichDrools._

    val f1_1 = FactOne("f1_1")
    session insert f1_1
    session fireAllRules
  }


}