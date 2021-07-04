package org.marid.types

import org.apache.commons.codec.binary.Hex
import java.nio.charset.StandardCharsets

@JvmRecord
data class VarName(private val name: String): Comparable<VarName> {
  val jvmName: String
    get() {
      val hex = Hex.encodeHexString(name.toByteArray(StandardCharsets.UTF_8))
      return "v_$hex"
    }

  override fun toString(): String = name
  override fun compareTo(other: VarName): Int = name.compareTo(other.name)
  val placeholder: String get() = "@{$name}"
}