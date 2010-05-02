package memelet.drools.scala.dialect

import org.drools.rule.builder.{RuleBuildContext, ConsequenceBuilder}
import org.drools.WorkingMemory
import org.drools.rule.Declaration
import scala.collection.mutable.Map
import org.drools.common.InternalWorkingMemory
import org.drools.reteoo.LeftTuple
import org.drools.spi.{KnowledgeHelper, Consequence}
import scala.collection.JavaConversions._

object ScalaStubConsequenceBuilder extends ConsequenceBuilder {

  def build(context: RuleBuildContext) {
    context.getBuildStack.push(context.getRule.getLhs)
    try {
      val declarations = context.getDeclarationResolver.getDeclarations(context.getRule)
      context.getRule.setConsequence(StubConsequence)
    } finally {
      context.getBuildStack.pop
    }
  }

  object StubConsequence extends Consequence {
    def evaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory) {
      throw new IllegalStateException("***TODO Stub not replaced")
    }
  }

  private abstract class ConsequenceInvoker(declarations: Map[String, Declaration]) extends Consequence {

    def invoke(facts: Map[String, (AnyRef, Option[Class[_]])])

    private def resolveClass(declaration: Declaration): Option[Class[_]] =
      Option(declaration.getExtractor.getExtractToClass)

    def evaluate(knowledgeHelper: KnowledgeHelper, workingMemory: WorkingMemory) {

      def resolveValue(declaration: Declaration): Option[AnyRef] = {
        val tuple = knowledgeHelper.getTuple
        val factHandles = tuple.asInstanceOf[LeftTuple].toFactHandles
        val offset: Int = declaration.getPattern.getOffset
        //TODO Is this check necessary?
        if (offset < factHandles.length) Some(factHandles(offset).getObject) else None
      }

      val facts: Map[String, (AnyRef, Option[Class[_]])] = for {
        (identifier, declaration) <- declarations
        value = resolveValue(declaration)
      } yield (identifier -> (value.get, resolveClass(declaration)))

      println("*** ConsequenceFunctionInvoker#evaluate(%s)".format(facts))
    }

  }

  private class Consequence0Invoker(declarations: Map[String, Declaration]) extends ConsequenceInvoker(declarations) {
    private[dialect] var consequence: Function0[Unit] = _
    def invoke(facts: Map[String, (AnyRef, Option[Class[_]])]) {
      consequence()
    }
  }

//  private class Consequence1Invoker(declarations: Map[String, Declaration]) extends ConsequenceInvoker(declarations) {
//    private[dialect] var consequence: Function1[_, Unit] = _
//    def invoke(facts: Map[String, (AnyRef, Option[Class[_]])]) {
//      consequence("foo")
//    }
//  }


}

