/*
 * MARID, the visual component programming environment.
 * Copyright (C) 2020 Dzmitry Auchynnikau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.marid.resolver

import org.apache.commons.codec.binary.Hex
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import java.util.regex.Pattern

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
        appendLine("${prefix}var ${jvmName(v)} = ${resolved(c)};")
      }
      return this
    }

    internal fun jvmName(v: String): String = "v_${Hex.encodeHexString(v.toByteArray(UTF_8))}"
    internal fun fromJvmName(v: String): String = String(Hex.decodeHex(v.substring(2)))
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