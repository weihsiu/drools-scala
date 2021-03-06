package memelet.drools.scala.dialect.embedded

import org.drools.rule.builder.dialect.mvel.MVELDialectConfiguration
import org.drools.compiler.{Dialect, PackageRegistry, PackageBuilder}

class ScalaEmbeddedDialectConfiguration extends MVELDialectConfiguration {

  override def newDialect(packageBuilder: PackageBuilder, pkgRegistry: PackageRegistry, pkg: org.drools.rule.Package): Dialect = {
    return new ScalaEmbeddedDialect(packageBuilder, pkgRegistry, pkg)
  }

//  override def init(conf: PackageBuilderConfiguration) = super.init(conf)
//  override def getPackageBuilderConfiguration = super.getPackageBuilderConfiguration
//  override def setStrict(strict: Boolean) = super.setStrict(strict)
//  override def isStrict = super.isStrict
//  override def setLangLevel(langLevel: Int) = super.setLangLevel(langLevel)
//  override def getLangLevel = super.getLangLevel
}