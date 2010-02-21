package memelet.drools.scala.evaluators

import java.io.{ObjectInput, ObjectOutput, Externalizable}
import org.drools.spi.Evaluator
import org.drools.base.ValueType
import org.drools.base.evaluators.Operator
import scala.collection.mutable.{Map,Set}

object EvaluatorCache {
  def apply(entries: (ValueType, Operator, Evaluator)*) = {
    val cache = new EvaluatorCache
    entries.foreach(entry => cache.add(entry._1, entry._2, entry._3))
    cache
  }
}

class EvaluatorCache extends Externalizable with Iterable[(ValueType, Map[Operator, Evaluator])] {

  private val cache = Map[ValueType, Map[Operator, Evaluator]]()

  def readExternal(in: ObjectInput) {
    in.readObject.asInstanceOf[Map[ValueType, Map[Operator, Evaluator]]].foreach { entry =>
      cache += ValueType.determineValueType(entry._1.getClassType) -> entry._2
    }
  }
  def writeExternal(out: ObjectOutput) { out.writeObject(cache) }

  def iterator = cache.iterator

  def add(vtype: ValueType, operator: Operator, evaluator: Evaluator) {
    cache.getOrElse(vtype, {
      val evaluators = Map[Operator, Evaluator]()
      cache += vtype -> evaluators
      evaluators
    }) += operator -> evaluator
  }

  def get(vtype: ValueType, operator: Operator): Option[Evaluator] = cache.get(vtype) match {
    case Some(evaluators) => evaluators.get(operator)
    case None => None
  }

  def supportsType(vtype: ValueType) = cache.contains(vtype)
}