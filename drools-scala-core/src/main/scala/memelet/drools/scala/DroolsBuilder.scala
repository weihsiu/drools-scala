package memelet.drools.scala

import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._
import org.joda.time.{Period, DateTime, DateTimeUtils}
import java.io.StringReader
import org.drools.{KnowledgeBaseConfiguration, FactException, KnowledgeBase, KnowledgeBaseFactory}
import org.drools.runtime.{KnowledgeSessionConfiguration, ObjectFilter, Globals, StatefulKnowledgeSession}
import org.drools.runtime.conf.{KnowledgeSessionOption, ClockTypeOption}
import org.drools.impl.EnvironmentImpl
//
import org.drools.event.rule._
import org.drools.time.{TimerService, SessionPseudoClock, SessionClock}
import org.drools.conf.{KnowledgeBaseOption}
import org.drools.runtime.rule.{WorkingMemoryEntryPoint, AgendaFilter, FactHandle}
import org.drools.io.{ResourceFactory, Resource}
import org.drools.definition.KnowledgePackage
import org.drools.builder.impl.KnowledgeBuilderImpl
import org.drools.builder.{KnowledgeBuilder, ResourceType, KnowledgeBuilderFactory}
import org.drools.builder.conf.EvaluatorOption

object ScalaExtensions {

  //TODO This doesn't seem to work.
  val scalaDrls = Seq("memelet/drools/scala/drools_scala_predef.drl")

  def defineScalaDrlElements(kbuilder: KnowledgeBuilder) {
    scalaDrls.foreach { filename =>
       kbuilder.add(ResourceFactory.newClassPathResource(filename), ResourceType.DRL)
    }
  }

  def setScalaGlobals(ksession: StatefulKnowledgeSession) {
    ksession.setGlobal("None", None)
  }

  /**
   * Evaluators are designed to be added via the 'drools.default.packagebuilder.conf' property file.
   * However, the order that evaluators are added by the runtime is indeterminate. which means that the
   * scala evalutors -- which override drools provided evaluators -- may get installed before the
   * drools evalators and hence be overriden.
   */
  def registerScalaEvaluators(kbuilder: KnowledgeBuilder) {
    val kbuilderConfig = kbuilder.asInstanceOf[KnowledgeBuilderImpl].getPackageBuilder.getPackageBuilderConfiguration
    val chainedProperties = kbuilderConfig.getChainedProperties
    val evaluatorRegistry = kbuilderConfig.getEvaluatorRegistry

    val scalaEntries = new java.util.HashMap[String, String]
    chainedProperties.mapStartsWith(scalaEntries, EvaluatorOption.PROPERTY_NAME+"scala_", true)
    for (className <- scalaEntries.values) {
      evaluatorRegistry.addEvaluatorDefinition(className)
    }
  }

}

object DroolsBuilder {

  def buildDrls(newResource: String => Resource, drls: Iterable[String]): Iterable[KnowledgePackage] = {
    val kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder
    ScalaExtensions.registerScalaEvaluators(kbuilder)

    ScalaExtensions.defineScalaDrlElements(kbuilder)
    drls foreach { drl => kbuilder.add(newResource(drl), ResourceType.DRL) }

    if (kbuilder.hasErrors) throw new RuntimeException(kbuilder.getErrors.mkString(","))
    kbuilder.getKnowledgePackages
  }

  def buildFileDrls(fileDrls: Iterable[String]): Iterable[KnowledgePackage] = buildDrls(ResourceFactory.newClassPathResource, fileDrls)

  def buildStringDrls(stringDrls: Iterable[String]): Iterable[KnowledgePackage] = {
    def newStringResource(drl: String) = ResourceFactory.newReaderResource(new StringReader(drl))
    buildDrls(newStringResource, stringDrls)
  }

  val emptyEnvironment = new EnvironmentImpl

  implicit def kbaseOptionsToKnowledgeBaseConfiguration(kbaseOptions: Seq[KnowledgeBaseOption]): KnowledgeBaseConfiguration = {
    val kbaseConfig = KnowledgeBaseFactory.newKnowledgeBaseConfiguration()
    kbaseOptions.foreach { kbaseConfig.setOption(_) }
    kbaseConfig
  }

  implicit def ksessionOptionsToKnowledgeSessionConfiguration(ksessionOptions: Seq[KnowledgeSessionOption]): KnowledgeSessionConfiguration = {
    val ksessionConfig = KnowledgeBaseFactory.newKnowledgeSessionConfiguration
    ksessionOptions.foreach { ksessionConfig.setOption(_) }
    ksessionConfig
  }
  
  def newKnowledgeBase(knowledgePackages: Iterable[KnowledgePackage], kbaseConfig: KnowledgeBaseConfiguration): KnowledgeBase = {
    val kbase = KnowledgeBaseFactory.newKnowledgeBase(kbaseConfig)
    kbase.addKnowledgePackages(knowledgePackages)
    kbase
  }
}

