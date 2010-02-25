package memelet.drools.scala.objectype

import org.drools.spi.ObjectType
import org.drools.base.ValueType
import java.io.{ObjectInput, ObjectOutput, Externalizable}

class ScalaObjectType extends ObjectType with Externalizable {

  def readExternal(in: ObjectInput): Unit = {
    throw new UnsupportedOperationException
  }

  def writeExternal(out: ObjectOutput): Unit = {
    throw new UnsupportedOperationException
  }

  def getValueType: ValueType = null

  def isEvent: Boolean = false

  def isAssignableFrom(objectType: ObjectType): Boolean = false
}