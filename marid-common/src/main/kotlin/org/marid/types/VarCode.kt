package org.marid.types

import java.util.*
import java.util.regex.Pattern

@JvmInline
value class VarCode(private val code: String) {

  val resolved: String get() = PATTERN.matcher(code).replaceAll { r -> VarName(r.group(1)).jvmName }
  fun contains(name: VarName): Boolean = code.contains(name.placeholder)
  val isRoot: Boolean get() = !PATTERN.matcher(code).find()
  override fun toString(): String = code

  val varNames: Set<VarName>
    get() {
      val treeSet = TreeSet<VarName>()
      val matcher = PATTERN.matcher(code)
      while (matcher.find()) {
        treeSet += VarName(matcher.group(1))
      }
      return treeSet
    }

  companion object {
    private val PATTERN: Pattern = Pattern.compile("@\\{([^}]+)}")
  }
}