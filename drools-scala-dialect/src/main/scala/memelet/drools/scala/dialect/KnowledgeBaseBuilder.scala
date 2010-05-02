package memelet.drools.scala.dialect

import org.drools.definition.KnowledgePackage
import java.io.StringReader
import org.drools.io.ResourceFactory
import org.drools.builder.{ResourceType, KnowledgeBuilderFactory}
import org.drools.definitions.impl.KnowledgePackageImp
import java.lang.reflect.Method
import com.thoughtworks.paranamer.{CachingParanamer, BytecodeReadingParanamer}
import org.drools.runtime.conf.ClockTypeOption
import org.drools.conf.EventProcessingOption
import memelet.drools.scala.{DroolsBuilder, ScalaExtensions}
import scala.collection.mutable
import org.drools.rule.builder.{RuleBuildContext, ConsequenceBuilder}
import org.drools.rule.{Declaration, Rule => DroolsRule}
import org.drools.WorkingMemory
import org.drools.spi.{KnowledgeHelper, Consequence, AgendaGroup}

import java.util.{Map => jMap}
import scala.collection.{Map => sMap}
import scala.collection.JavaConversions._

//TODO Maybe this should only yield compiled packages, leaving the creation of the kbase and session
// to the client.
class KnowledgeBaseBuilder {

  private[dialect] val paranamer = new CachingParanamer(new BytecodeReadingParanamer)
  private[dialect] val kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder
  ScalaExtensions.registerScalaEvaluators(kbuilder)

  def statefulSession = DroolsBuilder
            .buildKnowledgeBase(kbuilder.getKnowledgePackages, EventProcessingOption.STREAM)
            .statefulSession(ClockTypeOption.get("pseudo"))

}

object ScalaConsequenceBuilder extends ConsequenceBuilder {

  private[dialect] val builderStack = new mutable.Stack[Package#RuleBuilder]

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

class Package(name: String = null)(implicit val builder: KnowledgeBaseBuilder) {

  private var importDescrptors: Vector[String] = Vector.empty
  private def defaultSalience = () => 0

  def Import[T: Manifest] {
    importDescrptors = importDescrptors appendBack ("import "+manifest[T].erasure.getName+"\n")
  }

  case class Rule (name: String, salience: () => Int = defaultSalience, agendaGroup: Option[AgendaGroup] = None,
                   ruleflowGroup: Option[String] = None, lockOnActive: Boolean = false, noLoop: Boolean = false,
                   lhs: Option[String] = None) {

    def When(lhs: String): Rule = copy(lhs = Some(lhs))

    def Then(rhs: Function0[Unit]) { (new RuleBuilder0(this, rhs)).build }
    def Then[T1: Manifest](rhs: Function1[T1,Unit]) { (new RuleBuilder1(this, rhs)).build }
    def Then[T1: Manifest, T2: Manifest](rhs: Function2[T1,T2,Unit]) { (new RuleBuilder2(this, rhs)).build }

  }

  abstract class RuleBuilder(descriptor: Rule) {

    def packagedDrl: String = {
      """
      package %s

      %s

      import scala.Option
      global scala.None$ None

      rule "%s" dialect "scala"
        salience %d
      when
        %s
      then
        // placeholder for parser
      end
      """.format(Option(Package.this.name) getOrElse this.getClass.getPackage.getName,
                 importDescrptors reduceLeft (_ ++ _),
                 descriptor.name, descriptor.salience(), descriptor.lhs.get)
    }

    def consequence(declarations: jMap[String,Declaration]): Consequence
    
    def build {
      import builder.kbuilder
      ScalaConsequenceBuilder.builderStack.push(this)
      try {
        println(packagedDrl)
        kbuilder.add(ResourceFactory.newReaderResource(new StringReader(packagedDrl)), ResourceType.DRL)
        if (kbuilder.hasErrors) throw new RuntimeException(kbuilder.getErrors.mkString(","))
      } finally {
        ScalaConsequenceBuilder.builderStack.pop
      }
    }

    protected def lookupParameterNames(functionClass: Class[_]): Array[String] = {
      val applyMethod = functionClass.getMethods.filter(m => m.getName == "apply" && m.getReturnType == java.lang.Void.TYPE).head
      builder.paranamer.lookupParameterNames(applyMethod)
    }

    protected def parameterInfos(functionClass: Class[_]): Map[String,Class[_]] = {
      val applyMethod = functionClass.getMethods.filter(m => m.getName == "apply" && m.getReturnType == java.lang.Void.TYPE).head
      val parameterNames = builder.paranamer.lookupParameterNames(applyMethod)
      val parameterClasses = applyMethod.getParameterTypes
      (parameterNames zip parameterClasses) map { Map(_) } reduceLeft(_ ++ _)
    }
  }

  abstract class ConsequenceInvoker(declarations: sMap[String,Declaration]) extends Consequence {

    import org.drools.reteoo.LeftTuple

    protected def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: sMap[String,AnyRef])

    def evaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory) {

      def resolveValue(declaration: Declaration): Option[AnyRef] = {
        val tuple = knowledgeHelper.getTuple
        val factHandles = tuple.asInstanceOf[LeftTuple].toFactHandles
        val offset: Int = declaration.getPattern.getOffset
        //TODO Is this check necessary?
        if (offset < factHandles.length) Some(factHandles(offset).getObject) else None
      }

      val facts: sMap[String, AnyRef] = for {
        (identifier, declaration) <- declarations
        value = resolveValue(declaration)
      } yield (identifier -> value.get)

      doEvaluate(knowledgeHelper, workingMemory, facts)
    }

  }

  class RuleBuilder0(ruleDescriptor: Rule, rhs: Function0[Unit]) extends RuleBuilder(ruleDescriptor) {

    def consequence(declarations: jMap[String,Declaration]) = new ConsequenceInvoker(declarations) {
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: sMap[String,AnyRef]) {
        rhs()        
      }
    }

  }

  class RuleBuilder1[T1: Manifest](ruleDescriptor: Rule, rhs: Function1[T1,Unit]) extends RuleBuilder(ruleDescriptor) {

    def consequence(declarations: jMap[String,Declaration]) = new ConsequenceInvoker(declarations) {
      val parameterNames = lookupParameterNames(rhs.getClass)
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: sMap[String,AnyRef]) {
        rhs(facts(parameterNames(0)).asInstanceOf[T1])
      }
    }

  }

  class RuleBuilder2[T1: Manifest, T2: Manifest](ruleDescriptor: Rule, rhs: Function2[T1,T2,Unit]) extends RuleBuilder(ruleDescriptor) {

    def consequence(declarations: jMap[String,Declaration]) = new ConsequenceInvoker(declarations) {
      val parameterNames = lookupParameterNames(rhs.getClass)
      def doEvaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory, facts: sMap[String,AnyRef]) {
        rhs(facts(parameterNames(0)).asInstanceOf[T1], facts(parameterNames(1)).asInstanceOf[T2])
      }
    }

  }

}


//----------------------------------------------------------------------------------------------------------------------

class Function1Consequence[T1](f: Function1[T1,Unit]) { def consequence(p1: T1) = f.apply(p1) }
class Function2Consequence[T1,T2](f: Function2[T1,T2,Unit]) { def consequence(p1: T1, p2: T2) = f.apply(p1, p2) }
class Function3Consequence[T1,T2,T3](f: Function3[T1,T2,T3,Unit]) { def consequence(p1: T1, p2: T2, p3: T3) = f.apply(p1, p2, p3) }
class Function4Consequence[T1,T2,T3,T4](f: Function4[T1,T2,T3,T4,Unit]) { def consequence(p1: T1, p2: T2, p3: T3, p4: T4) = f.apply(p1, p2, p3, p4) }
class Function5Consequence[T1,T2,T3,T4,T5](f: Function5[T1,T2,T3,T4,T5,Unit]) { def consequence(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5) = f.apply(p1, p2, p3, p4, p5) }
class Function6Consequence[T1,T2,T3,T4,T5,T6](f: Function6[T1,T2,T3,T4,T5,T6,Unit]) { def consequence(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6) = f.apply(p1, p2, p3, p4, p5, p6) }
class Function7Consequence[T1,T2,T3,T4,T5,T6,T7](f: Function7[T1,T2,T3,T4,T5,T6,T7,Unit]) { def consequence(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6, p7: T7) = f.apply(p1, p2, p3, p4, p5, p6, p7) }

//  class RichExpression(expression: String) {
//    def rule: Rule = Rule(name = "foo")
//  }
//  implicit def enrichExpression(expression: String) = new RichExpression(expression)
//
//  class RichRule(rule: RuleWithWhen) {
////    class Function1Consequence[T1](f: Function1[T1,Unit]) { def consequence(p1: T1) = f.apply(p1) }
////    class Function2Consequence[T1,T2](f: Function2[T1,T2,Unit]) { def consequence(p1: T1, p2: T2) = f.apply(p1, p2) }
////    class Function3Consequence[T1,T2,T3](f: Function3[T1,T2,T3,Unit]) { def consequence(p1: T1, p2: T2, p3: T3) = f.apply(p1, p2, p3) }
////    class Function4Consequence[T1,T2,T3,T4](f: Function4[T1,T2,T3,T4,Unit]) { def consequence(p1: T1, p2: T2, p3: T3, p4: T4) = f.apply(p1, p2, p3, p4) }
////    class Function5Consequence[T1,T2,T3,T4,T5](f: Function5[T1,T2,T3,T4,T5,Unit]) { def consequence(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5) = f.apply(p1, p2, p3, p4, p5) }
////    class Function6Consequence[T1,T2,T3,T4,T5,T6](f: Function6[T1,T2,T3,T4,T5,T6,Unit]) { def consequence(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6) = f.apply(p1, p2, p3, p4, p5, p6) }
////    class Function7Consequence[T1,T2,T3,T4,T5,T6,T7](f: Function7[T1,T2,T3,T4,T5,T6,T7,Unit]) { def consequence(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6, p7: T7) = f.apply(p1, p2, p3, p4, p5, p6, p7) }
//
//    def ==>(f: ConsequenceFunction) = { rule.Then(f) }
////    def ==>[T1](f: Function1[T1,Unit]) = { rule.Then(new Function1Subscriber(f)); statement }
////    def ==>[T1,T2](f: Function2[T1,T2,Unit]) = { statement.setSubscriber(new Function2Subscriber(f)); statement }
////    def ==>[T1,T2,T3](f: Function3[T1,T2,T3,Unit]) = { statement.setSubscriber(new Function3Subscriber(f)); statement }
////    def ==>[T1,T2,T3,T4](f: Function4[T1,T2,T3,T4,Unit]) = { statement.setSubscriber(new Function4Subscriber(f)); statement }
////    def ==>[T1,T2,T3,T4,T5](f: Function5[T1,T2,T3,T4,T5,Unit]) = { statement.setSubscriber(new Function5Subscriber(f)); statement }
////    def ==>[T1,T2,T3,T4,T5,T6](f: Function6[T1,T2,T3,T4,T5,T6,Unit]) = { statement.setSubscriber(new Function6Subscriber(f)); statement }
////    def ==>[T1,T2,T3,T4,T5,T6,T7](f: Function7[T1,T2,T3,T4,T5,T6,T7,Unit]) = { statement.setSubscriber(new Function7Subscriber(f)); statement }
//  }
//  implicit def enrichRule(rule: RuleWithWhen) = new RichRule(rule)



