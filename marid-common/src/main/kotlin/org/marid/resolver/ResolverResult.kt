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

import com.google.common.collect.Maps
import org.eclipse.jdt.core.dom.ITypeBinding
import java.util.*

class ResolverResult {

  private val map = TreeMap<String, ITypeBinding>()

  internal fun add(name: String, type: ITypeBinding) {
    map[name] = type
  }

  fun toStringMap(): SortedMap<String, String> = Maps.transformValues(map) { toString(it!!) }

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
        if (v1 != v2) {
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
        val jvmName = Task.jvmName(k)
        val typeName = toString(v)
        val nullValue = when (typeName) {
          "int" -> "0"
          "long" -> "0L"
          "double" -> "0d"
          "float" -> "0f"
          "byte" -> "(byte) 0"
          "short" -> "(short) 0"
          "boolean" -> "false"
          "char" -> "(char) 0"
          else -> "null"
        }
        appendLine("${prefix}var $jvmName = ($typeName) $nullValue;")
      }
      return this
    }

    private fun toString(t: ITypeBinding): String {
      return when {
        t.isParameterizedType -> t.erasure.qualifiedName + t.typeArguments.joinToString(",", "<", ">") { toString(it) }
        t.isArray -> toString(t.componentType) + "[]"
        t.isIntersectionType -> t.toString()
        else -> t.qualifiedName
      }
    }
  }
}