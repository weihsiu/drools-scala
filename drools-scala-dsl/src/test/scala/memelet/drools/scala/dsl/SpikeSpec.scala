package memelet.drools.scala.dsl


import org.specs.SpecsMatchers
import org.specs.mock.Mockito
import org.drools.rule.VariableRestriction.{ObjectVariableContextEntry, VariableContextEntry}
import org.drools.base.evaluators.{EvaluatorDefinition, EvaluatorRegistry}
import org.junit.Test
import org.drools.rule._
import org.drools.base._
import field.ObjectFieldImpl
import org.drools.spi._
import org.drools.reteoo.builder.ReteooRuleBuilder
import memelet.drools.scala.{DroolsDebug, RichDrools, DroolsFixture}
import org.drools.reteoo.{ReteooStatefulSession, ReteooBuilder, ReteooRuleBase}
import org.drools.impl.{KnowledgeBaseImpl, StatefulKnowledgeSessionImpl}
import org.drools.runtime.StatefulKnowledgeSession
import org.drools.{KnowledgeBase, KnowledgeBaseFactory, WorkingMemory, RuleBaseConfiguration}
import org.drools.common.{InternalRuleBase, InternalWorkingMemory}

case class FactOne(name: String)
case class FactTwo(name: String, f: FactOne)

class SpikeSpec extends SpecsMatchers with Mockito {

  @Test def dslSpike {
    val drools = DroolsFixture(rules = Seq("memelet/drools/scala/dsl/spike_spec.drl"))
    import drools._
    import RichDrools._

    val f1_1 = FactOne("f1_1")
    val f1_2 = FactOne("f1_2")
    val f2_1 = FactTwo("f2_1", f1_1)
    session insert f1_1
    session insert f1_2
    session insert f2_1
    session fireAllRules
  }

  @Test def createKBase {

    val kbase: KnowledgeBase = KnowledgeBaseFactory.newKnowledgeBase()
    val rbase = kbase.asInstanceOf[KnowledgeBaseImpl].getRuleBase.asInstanceOf[InternalRuleBase]

    val reteooBuilder = rbase.getReteooBuilder
    val ruleBuilder = new ReteooRuleBuilder

    val classFieldAccessorCache = new ClassFieldAccessorCache(this.getClass.getClassLoader)
    val classFieldAccessorStore = new ClassFieldAccessorStore
    classFieldAccessorStore.setClassFieldAccessorCache(classFieldAccessorCache)
    val evaluatorRegistry = new EvaluatorRegistry

    val factOneObjectType = new ClassObjectType(classOf[FactOne])
    val factTwoObjectType = new ClassObjectType(classOf[FactTwo])

    val rule_1 = {
      val rule = new Rule("rule_1")
      val lhsroot = GroupElementFactory.newAndInstance

      val pattern_1 = {
        val pattern = new Pattern(0, factOneObjectType, "f1_1")
        val literalConstraint__FactOne_name = {
          val reader: ClassFieldReader = classFieldAccessorStore.getReader(classOf[FactOne], "name", null)
          val evaluator: Evaluator = evaluatorRegistry.getEvaluator(ValueType.STRING_TYPE, "==", false, null)
          val value: FieldValue = new ObjectFieldImpl("f1_1")
          val restriction: LiteralRestriction = new LiteralRestriction(value, evaluator, reader)
          val constraint = new LiteralConstraint(reader, restriction)
          constraint
        }
        pattern.addConstraint(literalConstraint__FactOne_name)
        pattern
      }
      lhsroot.addChild(pattern_1)

      val pattern_2 = {
        val pattern = new Pattern(0, factTwoObjectType, "f2_1")
        val literalConstraint__FactTwo_name = {
          val reader: ClassFieldReader = classFieldAccessorStore.getReader(classOf[FactTwo], "name", null)
          val evaluator: Evaluator = evaluatorRegistry.getEvaluator(ValueType.STRING_TYPE, "==", false, null)
          val value: FieldValue = new ObjectFieldImpl("f2_1")
          val restriction: LiteralRestriction = new LiteralRestriction(value, evaluator, reader)
          val constraint = new LiteralConstraint(reader, restriction)
          constraint
        }
        pattern.addConstraint(literalConstraint__FactTwo_name)

        val variableConstraint__FactTwo_f = {
          val reader: ClassFieldReader = classFieldAccessorStore.getReader(classOf[FactTwo], "f", null)
          val evaluator: Evaluator = evaluatorRegistry.getEvaluator(ValueType.OBJECT_TYPE, "==", false, null)
          val patternExtractor = new PatternExtractor(factTwoObjectType)
          val restriction: VariableRestriction = new VariableRestriction(reader, pattern_1.getDeclaration, evaluator)
          val constraint = new VariableConstraint(reader, restriction)
          constraint
        }
        pattern.addConstraint(variableConstraint__FactTwo_f)

        pattern
      }
      lhsroot.addChild(pattern_2)
      
      rule.setLhs(lhsroot)

      val consequence = new Consequence {
        def evaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory) = {
          println("*** consequence")
        }
      }
      rule.setConsequence(consequence)

      rule
    }
    ruleBuilder.addRule(rule_1, rbase, reteooBuilder.getIdGenerator)

    val ksession = kbase.newStatefulKnowledgeSession()
    DroolsDebug.debugWorkingMemory(ksession)
    DroolsDebug.debugAgenda(ksession)

    val f1_1 = FactOne("f1_1")
    val f2_1 = FactTwo("f2_1", f1_1)
    ksession insert f1_1
    ksession insert f2_1
    ksession fireAllRules

  }
  
}
