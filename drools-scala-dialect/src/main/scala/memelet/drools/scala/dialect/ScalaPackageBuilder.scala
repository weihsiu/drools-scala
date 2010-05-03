package memelet.drools.scala.dialect

import java.io.StringReader
import org.drools.io.ResourceFactory
import org.drools.builder.{ResourceType, KnowledgeBuilderFactory}
import org.drools.rule.builder.{RuleBuildContext, ConsequenceBuilder}
import org.drools.rule.Declaration
import org.drools.WorkingMemory
import org.drools.spi.{KnowledgeHelper, Consequence, AgendaGroup}

import com.thoughtworks.paranamer.BytecodeReadingParanamer
import memelet.drools.scala.ScalaExtensions
import scala.collection.mutable.{WeakHashMap, Stack}

import java.util.{Map => jMap}
import scala.collection.{Map => sMap}
import scala.collection.JavaConversions._

class ScalaPackageBuilder {

  private[dialect] val kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder
  ScalaExtensions.registerScalaEvaluators(kbuilder)

  private val paranamer = new BytecodeReadingParanamer
  private val paramInfoCache = new WeakHashMap[Class[_], Map[String,Class[_]]]

  private[dialect] def lookupParameterInfo(functionClazz: Class[_]): Map[String,Class[_]] = {

    paramInfoCache.get(functionClazz) getOrElse {
      val applyMethod = functionClazz.getMethods.filter(m => m.getName == "apply" && m.getReturnType == java.lang.Void.TYPE).head
      val paramNames = paranamer.lookupParameterNames(applyMethod)
      val paramTypes = applyMethod.getParameterTypes
      val paramInfo = Map.empty[String,Class[_]] ++ (paramNames zip paramTypes) map (e => e)
      paramInfoCache += (functionClazz -> paramInfo)
      paramInfo
    }
  }

  class RuleBuildException(message: String) extends RuntimeException(message)

  def knowledgePackages = kbuilder.getKnowledgePackages

}

private[dialect] object ScalaConsequenceBuilder extends ConsequenceBuilder {

  private[dialect] val builderStack = new Stack[Package#RuleBuilder]

  def build(context: RuleBuildContext) {
    context.getBuildStack.push(context.getRule.getLhs)
    try {
      val declarations = context.getDeclarationResolver.getDeclarations(context.getRule)
      val builder = builderStack.top
      context.getRule.setConsequence(builder.consequence(declarations))
    } finally {
      context.getBuildStack.pop
    }
  }
}

class Package(name: String = null)(implicit val builder: ScalaPackageBuilder) {

  import builder._

  private var ruleCount = 0
  private def generateRuleName = this.name + ".rule#" + (ruleCount+1)

  private var importDescriptors: Vector[String] = Vector.empty
  private def defaultSalience = 0

  def Import[T: Manifest] {
    importDescriptors = importDescriptors appendBack ("import "+manifest[T].erasure.getName+"\n")
  }

  //TODO Add remaining properties
  //TODO Dynamic salience (similar to MVELSalienceBuilder)
  //TODO Dynamic enabled (similar to MVELEnabledBuilder)
  case class Rule (name: String = generateRuleName, salience: Int = defaultSalience, agendaGroup: Option[String] = None,
                   ruleflowGroup: Option[String] = None, lockOnActive: Boolean = false, noLoop: Boolean = false) {

    ruleCount += 1
    private[dialect] var lhs: Option[String] = None

    def When(lhs: String): Rule = {
      this.lhs = Some(lhs)
      this
    }

    def Then(rhs: Function0[Unit]) { (new RuleBuilder0(this, rhs)).build }
    def Then[T1](rhs: Function1[T1,Unit]) { (new RuleBuilder1(this, rhs)).build }
    def Then[T1,T2](rhs: Function2[T1,T2,Unit]) { (new RuleBuilder2(this, rhs)).build }
    def Then[T1,T2,T3](rhs: Function3[T1,T2,T3,Unit]) { (new RuleBuilder3(this, rhs)).build }
    def Then[T1,T2,T3,T4](rhs: Function4[T1,T2,T3,T4,Unit]) { (new RuleBuilder4(this, rhs)).build }
    def Then[T1,T2,T3,T4,T5](rhs: Function5[T1,T2,T3,T4,T5,Unit]) { (new RuleBuilder5(this, rhs)).build }
    def Then[T1,T2,T3,T4,T5,T6](rhs: Function6[T1,T2,T3,T4,T5,T6,Unit]) { (new RuleBuilder6(this, rhs)).build }
    def Then[T1,T2,T3,T4,T5,T6,T7](rhs: Function7[T1,T2,T3,T4,T5,T6,T7,Unit]) { (new RuleBuilder7(this, rhs)).build }
    def Then[T1,T2,T3,T4,T5,T6,T7,T8](rhs: Function8[T1,T2,T3,T4,T5,T6,T7,T8,Unit]) { (new RuleBuilder8(this, rhs)).build }
    def Then[T1,T2,T3,T4,T5,T6,T7,T8,T9](rhs: Function9[T1,T2,T3,T4,T5,T6,T7,T8,T9,Unit]) { (new RuleBuilder9(this, rhs)).build }
    def Then[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10](rhs: Function10[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,Unit]) { (new RuleBuilder10(this, rhs)).build }

    def ==>(rhs: Function0[Unit]) { (new RuleBuilder0(this, rhs)).build }
    def ==>[T1](rhs: Function1[T1,Unit]) { (new RuleBuilder1(this, rhs)).build }
    def ==>[T1,T2](rhs: Function2[T1,T2,Unit]) { (new RuleBuilder2(this, rhs)).build }
    def ==>[T1,T2,T3](rhs: Function3[T1,T2,T3,Unit]) { (new RuleBuilder3(this, rhs)).build }
    def ==>[T1,T2,T3,T4](rhs: Function4[T1,T2,T3,T4,Unit]) { (new RuleBuilder4(this, rhs)).build }
    def ==>[T1,T2,T3,T4,T5](rhs: Function5[T1,T2,T3,T4,T5,Unit]) { (new RuleBuilder5(this, rhs)).build }
    def ==>[T1,T2,T3,T4,T5,T6](rhs: Function6[T1,T2,T3,T4,T5,T6,Unit]) { (new RuleBuilder6(this, rhs)).build }
    def ==>[T1,T2,T3,T4,T5,T6,T7](rhs: Function7[T1,T2,T3,T4,T5,T6,T7,Unit]) { (new RuleBuilder7(this, rhs)).build }
    def ==>[T1,T2,T3,T4,T5,T6,T7,T8](rhs: Function8[T1,T2,T3,T4,T5,T6,T7,T8,Unit]) { (new RuleBuilder8(this, rhs)).build }
    def ==>[T1,T2,T3,T4,T5,T6,T7,T8,T9](rhs: Function9[T1,T2,T3,T4,T5,T6,T7,T8,T9,Unit]) { (new RuleBuilder9(this, rhs)).build }
    def ==>[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10](rhs: Function10[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,Unit]) { (new RuleBuilder10(this, rhs)).build }
  }

  private[dialect] abstract class RuleBuilder(descriptor: Rule) {

    def quotedAttribute(name: String, value: Option[String]) = value match {
      case Some(value) => name + " \"" + value + "\""
      case None => ""
    }
    def booleanAttribute(name: String, value: Boolean) = name + " " + value

    def packagedDrl: String = {
      """
      package %s
      %s
      import scala.Option
      global scala.None$ None
      rule "%s" dialect "scala"
        salience %d
        %s
        %s
        %s
        %s
      when
        %s
      then
        // placeholder for parser
      end
      """.format(Option(Package.this.name) getOrElse Package.this.getClass.getPackage.getName,
                 if (importDescriptors.isEmpty) "" else importDescriptors reduceLeft (_ ++ _),
                 descriptor.name,
                 descriptor.salience,
                 quotedAttribute("agenda-group", descriptor.agendaGroup),
                 quotedAttribute("ruleflow-group", descriptor.ruleflowGroup),
                 booleanAttribute("no-loop", descriptor.noLoop),
                 booleanAttribute("lock-on-active", descriptor.lockOnActive),
                 descriptor.lhs.get)
    }

    def consequence(declaraions: jMap[String,Declaration]): Consequence

    def build {
      ScalaConsequenceBuilder.builderStack.push(this)
      try {
        kbuilder.add(ResourceFactory.newReaderResource(new StringReader(packagedDrl)), ResourceType.DRL)
        if (kbuilder.hasErrors) throw new RuleBuildException(kbuilder.getErrors.mkString(","))
      } finally {
        ScalaConsequenceBuilder.builderStack.pop
      }
    }

  }

  private abstract class TypedConsequence(declarations: sMap[String,Declaration], rhsClazz: Class[_]) extends Consequence {

    def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef])

    // Type check function parameters against declarations
    lookupParameterInfo(rhsClazz).foreach { paramInfo => paramInfo._2 match {
      case paramClazz if paramClazz.isAssignableFrom(classOf[KnowledgeHelper]) => //ok
      case paramClazz if paramClazz.isAssignableFrom(classOf[WorkingMemory]) => //ok
      case paramClazz => {
        val declaration = declarations.get(paramInfo._1) getOrElse {
          throw new RuleBuildException("Consequence parameter '%s: %s' does not match any fact identifiers"
            .format(paramInfo._1, paramInfo._2.getName))
        }
        val factClazz = declaration.getExtractor.getExtractToClass
        if (!paramClazz.isAssignableFrom(factClazz)) {
          throw new RuleBuildException("Consequence parameter '%s: %s' not assignable from Fact '%s: %s'"
            .format(paramInfo._1, paramInfo._2.getName, declaration.getIdentifier, factClazz.getName))
        }
      }
    }}

    def evaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory) {
      def facts: Seq[AnyRef] = //TODO Could this be (partially) cached somehow?
        Seq.empty[AnyRef] ++ lookupParameterInfo(rhsClazz).map { paramType => paramType._2 match {
          case pclazz if pclazz.isAssignableFrom(classOf[KnowledgeHelper]) => knowledgeHelper
          case pclazz if pclazz.isAssignableFrom(classOf[WorkingMemory]) => workingMemory
          case _ => knowledgeHelper.getTuple.get(declarations(paramType._1)).getObject
        }}
      doEvaluate(knowledgeHelper, workingMemory, facts)
    }

  }

  //TODO Find some way to kill this smelly boilerplate (without reflection)

  private class RuleBuilder0(ruleDescriptor: Rule, rhs: Function0[Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs()
      }
    }
  }

  private class RuleBuilder1[T1](ruleDescriptor: Rule, rhs: Function1[T1,Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs(facts(0).asInstanceOf[T1])
      }
    }
  }

  private class RuleBuilder2[T1,T2](ruleDescriptor: Rule, rhs: Function2[T1,T2,Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs(facts(0).asInstanceOf[T1], facts(1).asInstanceOf[T2])
      }
    }
  }

  private class RuleBuilder3[T1,T2,T3](ruleDescriptor: Rule, rhs: Function3[T1,T2,T3,Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs(facts(0).asInstanceOf[T1], facts(1).asInstanceOf[T2], facts(2).asInstanceOf[T3])
      }
    }
  }

  private class RuleBuilder4[T1,T2,T3,T4](ruleDescriptor: Rule, rhs: Function4[T1,T2,T3,T4,Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs(facts(0).asInstanceOf[T1], facts(1).asInstanceOf[T2], facts(2).asInstanceOf[T3], facts(3).asInstanceOf[T4])
      }
    }
  }

  private class RuleBuilder5[T1,T2,T3,T4,T5](ruleDescriptor: Rule, rhs: Function5[T1,T2,T3,T4,T5,Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs(facts(0).asInstanceOf[T1], facts(1).asInstanceOf[T2], facts(2).asInstanceOf[T3], facts(3).asInstanceOf[T4], facts(4).asInstanceOf[T5])
      }
    }
  }

  private class RuleBuilder6[T1,T2,T3,T4,T5,T6](ruleDescriptor: Rule, rhs: Function6[T1,T2,T3,T4,T5,T6,Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs(facts(0).asInstanceOf[T1], facts(1).asInstanceOf[T2], facts(2).asInstanceOf[T3], facts(3).asInstanceOf[T4], facts(4).asInstanceOf[T5],
            facts(5).asInstanceOf[T6])
      }
    }
  }

  private class RuleBuilder7[T1,T2,T3,T4,T5,T6,T7](ruleDescriptor: Rule, rhs: Function7[T1,T2,T3,T4,T5,T6,T7,Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs(facts(0).asInstanceOf[T1], facts(1).asInstanceOf[T2], facts(2).asInstanceOf[T3], facts(3).asInstanceOf[T4], facts(4).asInstanceOf[T5],
            facts(5).asInstanceOf[T6], facts(6).asInstanceOf[T7])
      }
    }
  }

  private class RuleBuilder8[T1,T2,T3,T4,T5,T6,T7,T8](ruleDescriptor: Rule, rhs: Function8[T1,T2,T3,T4,T5,T6,T7,T8,Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs(facts(0).asInstanceOf[T1], facts(1).asInstanceOf[T2], facts(2).asInstanceOf[T3], facts(3).asInstanceOf[T4], facts(4).asInstanceOf[T5],
            facts(5).asInstanceOf[T6], facts(6).asInstanceOf[T7], facts(7).asInstanceOf[T8])
      }
    }
  }

  private class RuleBuilder9[T1,T2,T3,T4,T5,T6,T7,T8,T9](ruleDescriptor: Rule, rhs: Function9[T1,T2,T3,T4,T5,T6,T7,T8,T9,Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs(facts(0).asInstanceOf[T1], facts(1).asInstanceOf[T2], facts(2).asInstanceOf[T3], facts(3).asInstanceOf[T4], facts(4).asInstanceOf[T5],
            facts(5).asInstanceOf[T6], facts(6).asInstanceOf[T7], facts(7).asInstanceOf[T8], facts(8).asInstanceOf[T9])
      }
    }
  }

  private class RuleBuilder10[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10](ruleDescriptor: Rule, rhs: Function10[T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,Unit]) extends RuleBuilder(ruleDescriptor) {
    def consequence(declarations: jMap[String,Declaration]) = new TypedConsequence(declarations, rhs.getClass) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: Seq[AnyRef]) {
        rhs(facts(0).asInstanceOf[T1], facts(1).asInstanceOf[T2], facts(2).asInstanceOf[T3], facts(3).asInstanceOf[T4], facts(4).asInstanceOf[T5],
            facts(5).asInstanceOf[T6], facts(6).asInstanceOf[T7], facts(7).asInstanceOf[T8], facts(8).asInstanceOf[T9], facts(9).asInstanceOf[T10])
      }
    }
  }

  //---- Sugar ----

  object RichDialect {
    
    class RichRule(rule: Rule) {

    }
    implicit def enrichRuleExpression(ruleExpression: String): RichRule = new RichRule(new Rule)

    implicit def stringToRule(ruleExpression: String): Rule = {
      val rule = new Rule
      rule.When(ruleExpression)
    }

  }


}
