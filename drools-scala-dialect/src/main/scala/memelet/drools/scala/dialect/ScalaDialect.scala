package memelet.drools.scala.dialect

import org.drools.rule.builder.dialect.mvel.MVELDialect
import org.drools.rule.builder.ConsequenceBuilder
import org.drools.compiler.{PackageBuilder, PackageRegistry}

class ScalaDialect(builder: PackageBuilder,
                   pkgRegistry: PackageRegistry,
                   pkg: org.drools.rule.Package)
        extends MVELDialect(builder, pkgRegistry, pkg, "scala") {

  override def getConsequenceBuilder: ConsequenceBuilder = ScalaConsequenceBuilder

}