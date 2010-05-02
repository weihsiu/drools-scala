package memelet.drools.scala.dialect

import org.drools.definition.KnowledgePackage
import java.io.StringReader
import org.drools.io.ResourceFactory
import org.drools.builder.{ResourceType, KnowledgeBuilderFactory}
import org.drools.definitions.impl.KnowledgePackageImp
import java.lang.reflect.Method
import com.thoughtworks.paranamer.{CachingParanamer, BytecodeReadingParanamer}
import scala.collection.JavaConversions._
import org.drools.runtime.conf.ClockTypeOption
import org.drools.conf.EventProcessingOption
import memelet.drools.scala.{DroolsBuilder, ScalaExtensions}
import org.drools.spi.{Consequence, AgendaGroup}
import org.drools.rule.Rule

class KnowledgeBaseBuilder {

  private var packageDeclarations: Vector[Package] = Vector.empty
  private[dialect] val paranamer = new CachingParanamer(new BytecodeReadingParanamer)
  private[dialect] val kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder
  ScalaExtensions.registerScalaEvaluators(kbuilder)

  private[dialect] def addPackage(packageDeclaration: Package) = packageDeclarations = packageDeclarations appendBack packageDeclaration

  def statefulSession = DroolsBuilder
            .buildKnowledgeBase(packageDeclarations.map(_.build), EventProcessingOption.STREAM)
            .statefulSession(ClockTypeOption.get("pseudo"))

}

case class Package(name: String = null)(implicit val builder: KnowledgeBaseBuilder) {

  builder.addPackage(this)

  private[dialect] var ruleDescriptors: Map[String,RuleWithConsequence] = Map.empty
  private var importDescrptors: Vector[String] = Vector.empty
  private def defaultSalience = () => 0

  def Import[T: Manifest] {
    importDescrptors = importDescrptors appendBack ("import "+manifest[T].erasure.getName+"\n")
  }

  def rule(name: String, salience: () => Int = defaultSalience, agendaGroup: Option[AgendaGroup] = None,
           ruleflowGroup: Option[String] = None, lockOnActive: Boolean = false, noLoop: Boolean = false,
           lhs: Option[String] = None) =
    RuleDescriptor(name, salience, agendaGroup, ruleflowGroup, lockOnActive, noLoop, lhs)

  case class RuleDescriptor(name: String, salience: () => Int = defaultSalience, agendaGroup: Option[AgendaGroup] = None,
                            ruleflowGroup: Option[String] = None, lockOnActive: Boolean = false, noLoop: Boolean = false,
                            lhs: Option[String] = None) {

    def when(lhs: String): RuleDescriptor = copy(lhs = Some(lhs))

    def then(rhs: Function0[Unit]) = RuleWithConsequence0(this, rhs)

    def drl = """
      rule "%s" dialect "scala"
        salience %d
      when
        %s
      then
        // placeholder for parser
      end
    """.format(name, salience(), lhs.get)

  }

  abstract class RuleWithConsequence(ruleDescriptor: RuleDescriptor) {

    ruleDescriptors = ruleDescriptors + (ruleDescriptor.name -> this)

    def drl = ruleDescriptor.drl

    protected[dialect] def replaceStubConsequence(rule: Rule, ruleDescriptor: RuleWithConsequence)

    protected def parameterInfos(functionClass: Class[_]): Map[String,Class[_]] = {
      val applyMethod = functionClass.getMethods.filter(m => m.getName == "apply" && m.getReturnType == java.lang.Void.TYPE).head
      val parameterNames = builder.paranamer.lookupParameterNames(applyMethod)
      val parameterClasses = applyMethod.getParameterTypes
      (parameterNames zip parameterClasses) map { Map(_) } reduceLeft(_ ++ _)
    }
  }

  case class RuleWithConsequence0(ruleDescriptor: RuleDescriptor, rhs: Function0[Unit]) extends RuleWithConsequence(ruleDescriptor) {

    protected[dialect] def replaceStubConsequence(rule: Rule, ruleDescriptor: RuleWithConsequence) {
      val parameterInfo = parameterInfos(rhs.getClass)
      println("** rule: %s(%s)".format(rule.getName, parameterInfo))
    }

  }

  def drl: String = """
    package %s
    %s
    import scala.Option
    global scala.None$ None
    %s
  """.format(Option(name) getOrElse this.getClass.getPackage.getName,
             importDescrptors reduceLeft (_ ++ _),
             ruleDescriptors map(_._2.drl) reduceLeft (_ ++ _)
      )

  def build: KnowledgePackage = {
    import builder.kbuilder

    println(drl)

    kbuilder.add(ResourceFactory.newReaderResource(new StringReader(drl)), ResourceType.DRL)
    if (kbuilder.hasErrors) throw new RuntimeException(kbuilder.getErrors.mkString(","))
    val pkg = kbuilder.getKnowledgePackages.head

    for (rule <- pkg.asInstanceOf[KnowledgePackageImp].pkg.getRules) {
//      replaceStubConsequence(rule, ruleDescriptors(rule.getName))
    }

    pkg
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



