package memelet.drools.scala.dialect.embedded
package test

import org.specs.SpecsMatchers
import org.specs.mock.Mockito
import org.drools.runtime.conf.ClockTypeOption
import org.drools.conf.EventProcessingOption
import org.drools.spi.KnowledgeHelper
import scala.collection.JavaConversions._
import org.junit.{Ignore, Test}
import org.drools.WorkingMemory
import org.drools.definitions.rule.impl.RuleImpl
import memelet.drools.scala._
import org.drools.definition.KnowledgePackage

class FactOne(val name: String)
class FactTwo(val name: String, val f: FactOne)
class FactThree(val name: String, val f: FactOne)
class FactFour(val name: String)
class FactOneSub(val subname: String) extends FactOne(subname)

class ScalaPackageBuilderSpec extends SpecsMatchers with Mockito {

  val f1_1 = new FactOne("f1_1#instance")
  val f1_2 = new FactOne("f1_2#instance")
  val f2_1 = new FactTwo("f2_1#instance", f1_1)
  val f2_2 = new FactTwo("f2_1#instance", f1_2)
  val f3_1 = new FactThree("f3_1#instance", f1_1)
  val f3_2 = new FactThree("f3_1#instance", f1_2)
  val f4_1 = new FactOne("f4_1#instance")
  val f4_2 = new FactOne("f4_2#instance")
  val f1s_1 = new FactOneSub("f1s_1#instance")

  var actual_f1_1: FactOne = _
  var actual_f1_2: FactOne = _
  var actual_f2_1: FactTwo = _
  var actual_f2_2: FactTwo = _
  var actual_f3_1: FactThree = _
  var actual_f3_2: FactThree = _
  var actual_f4_1: FactFour = _
  var actual_f4_2: FactFour = _

  import RichDrools._

  implicit val builder = new ScalaPackageBuilder
  lazy val session = {
    import DroolsBuilder._
    val session = newKnowledgeBase(builder.knowledgePackages, Seq(EventProcessingOption.STREAM))
                    .newScalaStatefulKnowledgeSession(Seq(ClockTypeOption.get("pseudo")), builder.globals)
    //DroolsDebug.debugWorkingMemory(session)
    //DroolsDebug.debugAgenda(session)
    session
  }

  def insertAllFactsAndFire {
    session insert f1_1
    session insert f1_2
    session insert f2_1
    session insert f2_2
    session insert f3_1
    session insert f3_2
    session insert f4_1
    session insert f4_2
    session fireAllRules
  }

  @Test def consequence_invoked_with_args {
    new DroolsPackage {
      Import[FactOne]
      Import[FactTwo]

      Rule("rule") when {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} then { (f1_1: FactOne, f2_1: FactTwo) =>
        actual_f1_1 = f1_1
        actual_f2_1 = f2_1
      }
    }

    insertAllFactsAndFire
    actual_f1_1 must_== f1_1
    actual_f2_1 must_== f2_1
  }

  @Test def consequence_invoked_with_zero_args {
    var invoked = false

    new DroolsPackage {
      Import[FactOne]

      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { () =>
        invoked = true
      }
    }

    insertAllFactsAndFire
    invoked must beTrue
  }

  @Test def consequence_invoked_with_args_when_args_are_out_of_order {
    new DroolsPackage {
      Import[FactOne]
      Import[FactTwo]

      Rule("rule") when {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} then { (f2_1: FactTwo, f1_1: FactOne) =>
        actual_f1_1 = f1_1
        actual_f2_1 = f2_1
      }
    }

    insertAllFactsAndFire
    actual_f1_1 must_== f1_1
    actual_f2_1 must_== f2_1
  }

  @Test def exception_thrown_for_missing_fact {
    new DroolsPackage {
      Import[FactOne]
      Import[FactTwo]

      Rule("rule") when {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} then { (f1_1: FactOne, f2_1: FactTwo, f3_1: FactThree) =>
        // noop
      }
    } must throwA[builder.RuleBuildException]
  }
  
  @Test def exception_thrown_for_arg_with_non_matching_names {
    new DroolsPackage {
      Import[FactOne]
      Import[FactTwo]

      Rule("rule") when {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} then { (f1_xxx: FactOne, f2_1: FactTwo, f3_1: FactThree) =>
        // noop
      }
    } must throwA[builder.RuleBuildException]
  }

  @Test def exception_thrown_for_arg_with_non_matching_type {
    new DroolsPackage {
      Import[FactOne]
      Import[FactTwo]

      Rule("rule") when {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} then { (f1_1: FactThree, f2_1: FactTwo) =>
        // noop
      }
    } must throwA[builder.RuleBuildException]
  }

  @Test def consequence_invoked_when_arg_supertype_of_fact {
    new DroolsPackage {
      Import[FactOneSub]

      Rule("rule") when {"""
        f1_1: FactOneSub()
      """} then { f1_1: FactOne =>
        actual_f1_1 = f1_1
      }
    }

    session insert f1s_1
    session fireAllRules()
    actual_f1_1 must_== f1s_1
  }
  
  @Test def exception_thrown_arg_subtype_of_fact { // really same as wrong type
    new DroolsPackage {
      Import[FactOne]

      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { f1_1: FactOneSub =>
        // noop
      }
    } must throwA[builder.RuleBuildException]
  }

  @Test def consequence_invoked_with_special_args {
    var actual_kh: KnowledgeHelper = null
    var actual_wm: WorkingMemory = null

    new DroolsPackage {
      Import[FactOne]

      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { (kh: KnowledgeHelper, wm: WorkingMemory) =>
        actual_kh = kh
        actual_wm = wm
      }
    }

    insertAllFactsAndFire
    actual_kh must notBeNull
    actual_wm must notBeNull
  }

  @Test def consequence_invoked_with_args_and_special_args {
    var actual_kh: KnowledgeHelper = null
    var actual_wm: WorkingMemory = null

    new DroolsPackage {
      Import[FactOne]
      Import[FactTwo]

      Rule("rule") when {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} then { (f1_1: FactOne, f2_1: FactTwo, kh: KnowledgeHelper, wm: WorkingMemory) =>
        actual_f1_1 = f1_1
        actual_f2_1 = f2_1
        actual_kh = kh
        actual_wm = wm
      }
    }

    insertAllFactsAndFire
    actual_f1_1 must_== f1_1
    actual_f2_1 must_== f2_1
    actual_kh must notBeNull
    actual_wm must notBeNull
  }

  @Test def exception_thrown_for_malformed_lhs {
    new DroolsPackage {
      Import[FactOne]

      Rule("rule") when {"""
        f1_1: FactOne() asdf asdf asdf asdf
      """} then { f1_1: FactOne =>
        // noop
      }
    } must throwA[builder.RuleBuildException]
  }

  // Really same as mismatch parameter name (wrt implementation)
  @Test def exception_thrown_for_missing_import {
    new DroolsPackage {
      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { f1_1: FactOneSub =>
        // noop
      }
    } must throwA[builder.RuleBuildException]
  }

  @Test def globals_added_to_session {
    new DroolsPackage {
      Global("factOne" -> new FactOne("f1_1"))
      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { () =>
        // noop
      }
    }

    val g = session.getGlobal("factOne").asInstanceOf[FactOne]
    g.name must_== "f1_1"
  }

  @Test def package_name_defaults_to_containing_scala_class_package {
    new DroolsPackage {
      Import[FactOne]
      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { () =>
        // noop
      }
    }

    builder.knowledgePackages.size must_== 1
    builder.knowledgePackages.head.getName must_== this.getClass.getPackage.getName
  }

  @Test def package_named_explicitly {
    new DroolsPackage("mypackage") {
      Import[FactOne]
      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { () =>
        // noop
      }
    }

    builder.knowledgePackages.size must_== 1
    builder.knowledgePackages.head.getName must_== "mypackage"
  }

  @Test def multiple_packages_with_different_names {
    new DroolsPackage("mypackage_1") {
      Import[FactOne]
      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { () =>
        // noop
      }
    }
    new DroolsPackage("mypackage_2") {
      Import[FactOne]
      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { () =>
        // noop
      }
    }

    builder.knowledgePackages.size must_== 2
    builder.knowledgePackages.map(_.getName) must haveTheSameElementsAs (List("mypackage_1", "mypackage_2"))
    builder.knowledgePackages.foreach { pkg =>
      pkg.getRules.size must_== 1
      pkg.getRules.head.getName must_== "rule"
    }
  }

  @Test def multiple_packages_with_same_name_combine {
    new DroolsPackage("mypackage") {
      Import[FactOne]
      Rule("rule_1") when {"""
        f1_1: FactOne()
      """} then { () =>
        // noop
      }
    }
    new DroolsPackage("mypackage") {
      Import[FactOne]
      Rule("rule_2") when {"""
        f1_1: FactOne()
      """} then { () =>
        // noop
      }
    }

    builder.knowledgePackages.size must_== 1
    builder.knowledgePackages.head.getName must_== "mypackage"
    builder.knowledgePackages.head.getRules.map(_.getName) must haveTheSameElementsAs (List("rule_1", "rule_2"))
  }

  // This is the default drools behavior, but it would be nice to get a warning.
  @Test def rule_with_same_name_overrides_previous {
    var rule1_invoked = false
    var rule2_invoked = false

    new DroolsPackage("mypackage") {
      Import[FactOne]
      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { () =>
        rule1_invoked = true
      }
      Rule("rule") when {"""
        f1_1: FactOne()
      """} then { () =>
        rule2_invoked = true
      }
    }

    insertAllFactsAndFire
    rule1_invoked must_== false
    rule2_invoked must_== true
  }

  @Test def rule_attributes_configured {
    new DroolsPackage {
      Import[FactOne]

      Rule("rule",
        salience = 10,
        agendaGroup = "agenda_group",
        ruleflowGroup = "ruleflow_group",
        lockOnActive = true,
        noLoop = true) 
      .when {"""
        f1_1: FactOne()
      """}
      .then { () =>
        // noop
      }
    }

    val rule = { //hack
      val ruleField = classOf[RuleImpl].getDeclaredField("rule")
      ruleField.setAccessible(true)
      ruleField.get(builder.knowledgePackages.head.getRules.head).asInstanceOf[org.drools.rule.Rule]
    }

    rule.getName must_== "rule"
    rule.getSalience.toString must_== "10" //hack
    rule.getAgendaGroup must_== "agenda_group"
    rule.getRuleFlowGroup must_== "ruleflow_group"
    rule.isLockOnActive must_== true
    rule.isNoLoop must_== true
  }

  @Test def auto_generated_rule_name {
    var rule1_invoked = false
    var rule2_invoked = false

    new DroolsPackage("mypackage") {
      Import[FactOne]
      Rule() when {"""
        f1_1: FactOne()
      """} then { () =>
        rule1_invoked = true
      }
      Rule() when {"""
        f1_1: FactOne()
      """} then { () =>
        rule2_invoked = true
      }
    }

    insertAllFactsAndFire
    // Don't care what the name is, just that its unique
    rule1_invoked must_== true
    rule2_invoked must_== true
  }

  @Test def multiline_syntax {
    var rule1_invoked = false
    var rule2_invoked = false

    new DroolsPackage("mypackage") {

      Import[FactOne]
      Import[FactTwo]

      Rule("rule1")
      .when{"""
        f1_1: FactOne()
        f2_1: FactTwo()"""}
      .then { (f1_1: FactOne, f2_1: FactTwo) =>
        rule1_invoked = true
      }
    }

    insertAllFactsAndFire
    rule1_invoked must_== true
  }

  @Test def concise_syntax {
    var rule1_invoked = false
    var rule2_invoked = false

    new DroolsPackage("mypackage") {

      Import[FactOne]
      Import[FactTwo]

      Rule("rule1") {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} ==> { (f1_1: FactOne, f2_1: FactTwo) =>
        rule1_invoked = true
      }

      Rule("rule2", salience = 10) {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} ==> { (f1_1: FactOne, f2_1: FactTwo) =>
        rule2_invoked = true
      }
    }

    insertAllFactsAndFire
    rule1_invoked must_== true
    rule2_invoked must_== true
  }

  @Test def non_inner_rule_syntax {
    var rule1_invoked = false
    var rule2_invoked = false

    val p = new DroolsPackage("mypackage") {
      Import[FactOne]
      Import[FactTwo]
    }

    p.Rule("p.rule1") {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} ==> { (f1_1: FactOne, f2_1: FactTwo) =>
        rule1_invoked = true
      }

    p.Rule("p.rule2", salience = 10) {"""
      f1_1: FactOne()
      f2_1: FactTwo()
    """} ==> { (f1_1: FactOne, f2_1: FactTwo) =>
      rule2_invoked = true
    }

    insertAllFactsAndFire
    rule1_invoked must_== true
    rule2_invoked must_== true
  }

  @Test def non_inner_rule_using_import_syntax {
    var rule1_invoked = false
    var rule2_invoked = false

    val p = new DroolsPackage("mypackage") {
      Import[FactOne]
      Import[FactTwo]
    }
    import p.Rule

    Rule("p.rule1") {"""
        f1_1: FactOne()
        f2_1: FactTwo()
      """} ==> { (f1_1: FactOne, f2_1: FactTwo) =>
        rule1_invoked = true
      }

    Rule("p.rule2", salience = 10) {"""
      f1_1: FactOne()
      f2_1: FactTwo()
    """} ==> { (f1_1: FactOne, f2_1: FactTwo) =>
      rule2_invoked = true
    }

    insertAllFactsAndFire
    rule1_invoked must_== true
    rule2_invoked must_== true
  }

  @Test def single_package_syntax {

    val p = new DroolsPackage {

      Import[FactOne]
      Import[FactTwo]

      Rule("rule1")
      .when{"""
        f1_1: FactOne()
        f2_1: FactTwo()"""}
      .then { (f1_1: FactOne, f2_1: FactTwo) =>
        //
      }
    }

    val kp: KnowledgePackage = p.knowledgePackage
    kp.getRules filter (_.getName == "rule1") must notBeNull

  }

  @Test def single_package_subclass_syntax {

    class MyRules extends DroolsPackage {

      Import[FactOne]
      Import[FactTwo]

      Rule("rule1")
      .when{"""
        f1_1: FactOne()
        f2_1: FactTwo()"""}
      .then { (f1_1: FactOne, f2_1: FactTwo) =>
        //
      }
    }

    val kp: KnowledgePackage = (new MyRules).knowledgePackage
    kp.getRules filter (_.getName == "rule1") must notBeNull

  }


}
