package memelet.drools.scala.dialect.embedded

import org.junit.Test
import memelet.drools.scala.DroolsFixture

case class A(val msg: String = "world")
case class B(val content: List[A])
case class JB(val content: java.util.List[A])

class FromClauseScalaLimitations {

  implicit val builder = new ScalaPackageBuilder()

  /**
   * Scala collections cannot be used in the from clause.
   */
  @Test(expected=classOf[ClassCastException])
  def mvel_with_from_scala_list {
    val drools = DroolsFixture(drls = Seq("memelet/drools/scala/dialect/embedded/mvel_with_from_scala_list.drl"))
    import drools._

    session insert B(List(A("me"),A("you"),A("world")))
	  session.fireAllRules
  }

  @Test
  def mvel_with_from_java_list {
    val drools = DroolsFixture(drls = Seq("memelet/drools/scala/dialect/embedded/mvel_with_from_java_list.drl"))
    import drools._

    val bl = new java.util.ArrayList[A]()
    bl.add(A("me"))
    bl.add(A("you"))
    bl.add(A("world"))
    session insert JB(bl)
	  session.fireAllRules
  }
}