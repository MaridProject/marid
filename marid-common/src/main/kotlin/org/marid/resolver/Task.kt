package org.marid.resolver

import org.apache.commons.codec.binary.Hex
import org.apache.commons.text.StringEscapeUtils
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import java.util.regex.Pattern
import kotlin.jvm.Throws

class Task {

  private val mapping = LinkedHashMap<String, String>()

  fun add(varName: String, varCode: String): Task {
    mapping[varName] = varCode
    return this
  }

  val isEmpty: Boolean get() = mapping.isEmpty()
  val isNotEmpty: Boolean get() = mapping.isNotEmpty()

  @Throws(CircularReferenceException::class)
  fun toLayers(): List<Task> {
    val rootTask = Task().also {
      for ((k, v) in mapping) {
        if (isRoot(v)) {
          it.mapping[k] = v
        }
      }
    }
    return if (rootTask.isNotEmpty) {
      val map = LinkedHashMap(mapping).also { it.keys.removeAll(rootTask.mapping.keys) }
      val list = arrayListOf(rootTask)
      while (map.isNotEmpty()) {
        val task = Task().also {
          for ((k, v) in map) {
            val varNames = varNames(v)
            if (varNames.all { n -> list.any { t -> t.mapping.containsKey(n) } }) {
              it.mapping[k] = v
            }
          }
        }
        if (task.isEmpty) {
          throw CircularReferenceException(list, Task().also { it.mapping.putAll(map) })
        } else {
          map.keys.removeAll(task.mapping.keys)
          list += task
        }
      }
      list
    } else {
      emptyList()
    }
  }

  override fun toString(): String = mapping.toString()
  override fun hashCode(): Int = mapping.hashCode()
  override fun equals(other: Any?): Boolean = when (other) {
    is Task -> this === other || mapping == other.mapping
    else -> false
  }

  companion object {

    private val PATTERN: Pattern = Pattern.compile("@\\{([^}]+)}")

    internal fun <T: Appendable> T.append(prefix: String, t: Task): T {
      t.mapping.forEach { (v, c) ->
        appendLine("$prefix@Var(\"${escapedName(v)}\") var ${jvmName(v)} = ${resolved(c)};")
      }
      return this
    }

    internal fun jvmName(v: String): String = "v_${Hex.encodeHexString(v.toByteArray(UTF_8))}"
    internal fun escapedName(v: String): String = StringEscapeUtils.escapeJava(v)
    private fun placeholder(v: String): String = "@{$v}"

    private fun varNames(code: String): Set<String> {
      val treeSet = TreeSet<String>()
      val matcher = PATTERN.matcher(code)
      while (matcher.find()) {
        treeSet += matcher.group(1)
      }
      return treeSet
    }

    private fun isRoot(code: String): Boolean = !PATTERN.matcher(code).find()
    private fun contains(code: String, v: String): Boolean = code.contains(placeholder(v))
    private fun resolved(code: String): String = PATTERN.matcher(code).replaceAll { r -> jvmName(r.group(1)) }

    class CircularReferenceException(val tasks: List<Task>, val rest: Task): Exception("Circular reference: $rest")
  }
}