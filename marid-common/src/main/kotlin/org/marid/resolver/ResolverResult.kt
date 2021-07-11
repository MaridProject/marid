package org.marid.resolver

import org.eclipse.jdt.internal.compiler.lookup.TypeBinding
import java.util.*

class ResolverResult {

  private val map = TreeMap<String, TypeBinding>()

  internal fun add(name: String, type: TypeBinding) {
    map[name] = type
  }

  fun toString(name: String): String = map[name]?.toString() ?: ""
  override fun toString(): String = map.toString()
  override fun hashCode(): Int = map.hashCode()
  override fun equals(other: Any?): Boolean {
    if (other is ResolverResult) {
      if (this === other) {
        return true
      }
      if (other.map.keys != map.keys) {
        return false
      }
      for ((k, v1) in map) {
        val v2 = other.map[k]
        if (TypeBinding.notEquals(v1, v2)) {
          return false
        }
      }
      return true
    } else {
      return false
    }
  }

  companion object {

    internal fun <T: Appendable> T.append(prefix: String, result: ResolverResult): T {
      for ((k, v) in result.map) {
        val annName = Task.escapedName(k)
        val jvmName = Task.jvmName(k)
        appendLine("$prefix@Var(\"$annName\") var $jvmName = ($v) null;")
      }
      return this
    }
  }
}