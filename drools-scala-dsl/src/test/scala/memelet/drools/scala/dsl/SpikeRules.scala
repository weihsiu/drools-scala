package memelet.drools.scala.dsl

object Rules {

  case class AgendaGroup(name: String)
  implicit def stringToSomeAgendaGroup(name: String): Option[AgendaGroup] = Some(AgendaGroup(name))
  implicit def agendaGroupToSomeAgendaGroup(agendaGroup: AgendaGroup): Option[AgendaGroup] = Some(agendaGroup)

  case class Rule(
          name: String,
          salience: Int = 0,
          agendaGroup: Option[AgendaGroup] = None,
          ruleflowGroup: Option[String] = None,
          lockOnActive: Boolean = false,
          noLoop: Boolean = false
          )
  {
    def then (consequence: => Unit) { consequence }
  }

  class Evaluator

  case class Expression(property: Symbol, matcher: Evaluator)

  class PatternResult[T: Manifest] {
    def value: T = throw new UnsupportedOperationException
    def apply: T = value
  }
  implicit def patternResultToValue[T: Manifest](patternResult: PatternResult[T]) = patternResult.value

  abstract class PatternDsl[T: Manifest] {
    def apply(expressions: Expression*): PatternResult[T] = {
      val result = new PatternResult[T]
      result
    }
  }

  class SymbolDsl(symbol: Symbol) {
    def is_==(value: Any) = Expression(symbol, new Evaluator)
    def is_!=(value: Any) = Expression(symbol, new Evaluator)
    def is_eq(result: PatternResult[_]) = Expression(symbol, new Evaluator)
    def is_same = is_eq _
    def contains(values: Set[Any]) = Expression(symbol, new Evaluator)
  }
  implicit def symbolDsl(symbol: Symbol): SymbolDsl = new SymbolDsl(symbol)
}



class SpikeRules {

  import memelet.drools.scala.dsl.Rules._

  class FactOne
  class FactTwo {
    val things: Seq[String] = Seq("one", "two")
  }
  class FactThree

  object AgendaXXX extends AgendaGroup("XXX")
  object AgendaYYY extends AgendaGroup("XXX")

  object FactOne extends PatternDsl[FactOne]
  object FactTwo extends PatternDsl[FactTwo]
  object FactThree extends PatternDsl[FactThree]

  private def doStuff(f1: FactOne, f2: FactTwo) { /*...*/ }

  //-----------------------------------------------------
  object RuleX extends Rule (
    name = "ruleX",
    agendaGroup = AgendaXXX)
  {
    val f1 = FactOne('foo is_== "bar", 'bla is_!= "blaz")
    val f2 = FactTwo('f1 is_same f1)
    FactThree('blazz contains Set("X", "Y"))

    then {
      f2.things.filter(_ == "bar")
    }
  }
  //-----------------------------------------------------

  object RuleY extends Rule (
    name = "ruleY",
    agendaGroup = "YYY")
  {
    val f1 = FactOne('foo is_== "bar", 'bla is_!= "blaz")
    val f2 = FactTwo('f1 is_same f1)
    FactThree('blazz contains Set("X", "Y"))

    then {
      doStuff(f1, f2)
    }

  }

}