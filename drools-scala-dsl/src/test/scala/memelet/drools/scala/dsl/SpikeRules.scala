package memelet.drools.scala.dsl

object Rules {

//  case class AgendaGroup(name: String)
//  implicit def stringToSomeAgendaGroup(name: String): Option[AgendaGroup] = Some(AgendaGroup(name))
//  implicit def agendaGroupToSomeAgendaGroup(agendaGroup: AgendaGroup): Option[AgendaGroup] = Some(agendaGroup)
//
//  case class Rule(
//          name: String,
//          salience: Int = 0,
//          agendaGroup: Option[AgendaGroup] = None,
//          ruleflowGroup: Option[String] = None,
//          lockOnActive: Boolean = false,
//          noLoop: Boolean = false
//          )
//  {
//    def then (consequence: => Unit) { consequence }
//  }
//
//  class DslEvaluator
//
//  case class DslConstraint(property: Symbol, matcher: DslEvaluator)
//
//  class DslDeclaration[T: Manifest] {
//    def value: T = throw new UnsupportedOperationException
//    def apply: T = value
//  }
//  implicit def dslDeclarationToValue[T: Manifest](dslDeclaration: DslDeclaration[T]) = dslDeclaration.value
//
//  abstract class DslPattern[T: Manifest] {
//    def apply(constraints: DslConstraint*): DslDeclaration[T] = {
//      val result = new DslDeclaration[T]
//      result
//    }
//  }
//
//  class SymbolDsl(symbol: Symbol) {
//    def ~==(value: Any) = DslConstraint(symbol, new DslEvaluator)
//    def ~!=(value: Any) = DslConstraint(symbol, new DslEvaluator)
//    def ~==(result: DslDeclaration[_]) = DslConstraint(symbol, new DslEvaluator)
//    def contains(values: Set[Any]) = DslConstraint(symbol, new DslEvaluator)
//  }
//  implicit def symbolDsl(symbol: Symbol): SymbolDsl = new SymbolDsl(symbol)
//}
//
//
//
//class SpikeRules {
//
//  import memelet.drools.scala.dsl.Rules._
//
//  class FactOne
//  class FactTwo {
//    val things: Seq[String] = Seq("one", "two")
//  }
//  class FactThree
//
//  object AgendaXXX extends AgendaGroup("XXX")
//  object AgendaYYY extends AgendaGroup("XXX")
//
//  object FactOne extends DslPattern[FactOne]
//  object FactTwo extends DslPattern[FactTwo]
//  object FactThree extends DslPattern[FactThree]
//
//  private def doStuff(f1: FactOne, f2: FactTwo) { /*...*/ }
//
//  //-----------------------------------------------------
//  object RuleX extends Rule (
//    name = "ruleX",
//    agendaGroup = AgendaXXX)
//  {
//    val f1 = FactOne('foo ~== "bar", 'bla ~!= "blaz")
//    val f2 = FactTwo('f1 ~== f1)
//    FactThree('blazz contains Set("X", "Y"))
//
//    then {
//      f2.things.filter(_ == "bar")
//    }
//  }
//  //-----------------------------------------------------
//
//  object RuleY extends Rule (
//    name = "ruleY",
//    agendaGroup = "YYY")
//  {
//    val f1 = FactOne('foo ~== "bar", 'bla ~!= "blaz")
//    val f2 = FactTwo('f1 ~== f1)
//    FactThree('blazz contains Set("X", "Y"))
//
//    then {
//      doStuff(f1, f2)
//    }
//
//  }

}