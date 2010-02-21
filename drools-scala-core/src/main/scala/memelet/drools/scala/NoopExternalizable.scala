package memelet.drools.scala

import java.io.{ObjectOutput, ObjectInput, Externalizable}

trait NoopExternalizable extends Externalizable {
  def readExternal(in: ObjectInput) { /*noop*/ }
  def writeExternal(out: ObjectOutput) { /*noop*/ }
}

