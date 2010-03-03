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
import org.drools.impl.{KnowledgeBaseImpl, StatefulKnowledgeSessionImpl}
import org.drools.runtime.StatefulKnowledgeSession
import org.drools.{KnowledgeBase, KnowledgeBaseFactory, WorkingMemory, RuleBaseConfiguration}
import org.drools.reteoo.{LeftTuple, ReteooStatefulSession, ReteooBuilder, ReteooRuleBase}
import org.drools.common.{InternalFactHandle, InternalRuleBase, InternalWorkingMemory}
import util.DynamicVariable

case class FactOneX(name: String)
case class FactTwoX(name: String, f: FactOneX)

class SpikeSpec extends SpecsMatchers with Mockito {

  @Test def dslSpike {
    val drools = DroolsFixture(rules = Seq("memelet/drools/scala/dsl/spike_spec.drl"))
    import drools._
    import RichDrools._

    val f1_1 = FactOneX("f1_1")
    val f1_2 = FactOneX("f1_2")
    val f2_1 = FactTwoX("f2_1", f1_1)
    session insert f1_1
    session insert f1_2
    session insert f2_1
    session fireAllRules
  }

  //--------------------------------------------------------------------------------------------------------------------

  val classFieldAccessorStore = new ClassFieldAccessorStore
  classFieldAccessorStore.setClassFieldAccessorCache(new ClassFieldAccessorCache(this.getClass.getClassLoader))
  val evaluatorRegistry = new EvaluatorRegistry

  case class DslRule(
          name: String,
          salience: Int = 0,
          agendaGroup: Option[AgendaGroup] = None,
          ruleflowGroup: Option[String] = None,
          lockOnActive: Boolean = false,
          noLoop: Boolean = false
          )
      extends org.drools.rule.Rule(name)
  { rule =>

    private var declarationIndex = 0
    def nextDeclarationIndex = {
      declarationIndex += 1
      declarationIndex
    }

    implicit val self: DslRule = this

    private val consequenceFacts = new DynamicVariable[Array[InternalFactHandle]](null)
    def facts = consequenceFacts.value

    class DslConsequence(f: => Unit) extends Consequence {
      def evaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory) = {
        consequenceFacts.withValue(knowledgeHelper.getTuple.asInstanceOf[LeftTuple].toFactHandles) {
          f
        }
      }
    }

    implicit def dslDeclarationToValue[T](dslDeclaration: DslDeclaration[T]) = dslDeclaration.value
    
    def then (f: => Unit) { this.setConsequence(new DslConsequence(f)) }
  }

  class DslDeclaration[T](val underlying: Declaration) {
    def value(implicit rule: DslRule): T = {
      val handle = rule.facts(underlying.getPattern.getOffset)
      handle.getObject.asInstanceOf[T]
    }
  }

  class DslPattern[T: Manifest] {
    val objectType = new ClassObjectType(manifest[T].erasure)
    def classType = objectType.getClassType
    var underlying: Pattern = _
    var declaration: DslDeclaration[T] = _

    def apply(constraints: (DslPattern[T] => Constraint)*)(implicit rule: DslRule): DslDeclaration[T] = {
      val index = rule.nextDeclarationIndex
      underlying = new Pattern(index, index, objectType, index.toString)
      declaration = new DslDeclaration(underlying.getDeclaration)

      constraints.foreach{ c =>
        underlying.addConstraint(c(this))
      }

      rule.addPattern(underlying)

      declaration
    }
  }

  def LiteralConstraint[T](dslPattern: DslPattern[T])(property: Symbol, evaluator: Evaluator, value: Any): Constraint = {
    val reader: ClassFieldReader = classFieldAccessorStore.getReader(dslPattern.classType, property.name, null)
    val restriction: LiteralRestriction = new LiteralRestriction(new ObjectFieldImpl(value), evaluator, reader)
    val constraint = new LiteralConstraint(reader, restriction)
    constraint
  }

  def VariableConstraint[T](dslPattern: DslPattern[T])(property: Symbol, evaluator: Evaluator, declaration: Declaration): Constraint = {
    val reader: ClassFieldReader = classFieldAccessorStore.getReader(dslPattern.classType, property.name, null)
    val restriction: VariableRestriction = new VariableRestriction(reader, declaration, evaluator)
    val constraint = new VariableConstraint(reader, restriction)
    constraint
  }

  class DslSymbol(symbol: Symbol) {
    def ~==(value: String) = LiteralConstraint(_:DslPattern[_])(symbol, evaluatorRegistry.getEvaluator(ValueType.OBJECT_TYPE, "==", false, null), value)
    def ~!=(value: String) = LiteralConstraint(_:DslPattern[_])(symbol, evaluatorRegistry.getEvaluator(ValueType.OBJECT_TYPE, "!=", false, null), value)
    def ~==(declaration: DslDeclaration[_]) = VariableConstraint(_:DslPattern[_])(symbol, evaluatorRegistry.getEvaluator(ValueType.OBJECT_TYPE, "==", false, null), declaration.underlying)
  }
  implicit def dslSymbol(symbol: Symbol): DslSymbol = new DslSymbol(symbol)



  case class FactOne(name: String)
  case class FactTwo(name: String, f: FactOne) {
      val things: Seq[String] = Seq("one", "two")
    }

  object FactOne extends DslPattern[FactOne]
  object FactTwo extends DslPattern[FactTwo]

  private def doStuff(f1: FactOne, f2: FactTwo) { /*...*/ }

  //-----------------------------------------------------
  class RuleX extends DslRule (
    name = "ruleX")
  {
    val f1 = FactOne('name ~== "f1_1")
    val f2 = FactTwo('f ~== f1)

    then {
      f2.things.filter(_ == "bar")
    }
  }
  //-----------------------------------------------------

  @Test def createFromDsl {

    val kbase: KnowledgeBase = KnowledgeBaseFactory.newKnowledgeBase()
    val rbase = kbase.asInstanceOf[KnowledgeBaseImpl].getRuleBase.asInstanceOf[InternalRuleBase]

    val reteooBuilder = rbase.getReteooBuilder
    val ruleBuilder = new ReteooRuleBuilder

    val ruleX = new RuleX
    ruleBuilder.addRule(ruleX, rbase, reteooBuilder.getIdGenerator)

    val ksession = kbase.newStatefulKnowledgeSession()
    DroolsDebug.debugWorkingMemory(ksession)
    DroolsDebug.debugAgenda(ksession)

    val f1_1 = FactOne("f1_1")
    val f2_1 = FactTwo("f2_1", f1_1)
    ksession insert f1_1
    ksession insert f2_1
    ksession fireAllRules
    
  }

  @Test def createKBase {

    case class FactOne(name: String)
    case class FactTwo(name: String, f: FactOne)

    //---- ---- ---

    val kbase: KnowledgeBase = KnowledgeBaseFactory.newKnowledgeBase()
    val rbase = kbase.asInstanceOf[KnowledgeBaseImpl].getRuleBase.asInstanceOf[InternalRuleBase]

    val reteooBuilder = rbase.getReteooBuilder
    val ruleBuilder = new ReteooRuleBuilder

    val rule_1 = new DslRule("rule_1") {
        val pattern_1 = new DslPattern[FactOne]
        val pattern_1_decl = pattern_1.apply(
          LiteralConstraint(_:DslPattern[_])('name, evaluatorRegistry.getEvaluator(ValueType.OBJECT_TYPE, "==", false, null), "f1_1")
        )
        addPattern(pattern_1.underlying)

        val pattern_2 = new DslPattern[FactTwo]
        val pattern_2_decl = pattern_2.apply(
          LiteralConstraint(_:DslPattern[_])('name, evaluatorRegistry.getEvaluator(ValueType.OBJECT_TYPE, "==", false, null), "f2_1"),
          VariableConstraint(_:DslPattern[_])('f, evaluatorRegistry.getEvaluator(ValueType.OBJECT_TYPE, "==", false, null), pattern_1_decl.underlying)
        )
        addPattern(pattern_2.underlying)

        val consequence = new Consequence {
          def evaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory) = {
            println("*** consequence")
          }
        }
        setConsequence(consequence)
    }
    ruleBuilder.addRule(rule_1, rbase, reteooBuilder.getIdGenerator)

    //----

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

