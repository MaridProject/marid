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
  private val errors = ArrayList<Throwable>()

  internal fun add(name: String, type: ITypeBinding) {
    map[name] = type
  }

  internal fun addError(error: Throwable) {
    if (errors.none { deepEquals(it, error) }) {
      errors += error
    }
  }

  val hasErrors: Boolean get() = errors.isNotEmpty()

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
    private fun toString(t: ITypeBinding): String {
      return when {
        t.isParameterizedType -> t.erasure.qualifiedName + t.typeArguments.joinToString(",", "<", ">") { toString(it) }
        t.isArray -> toString(t.componentType) + "[]"
        t.isIntersectionType -> (listOf(t.bound) + t.interfaces).joinToString("&") { toString(it) }
        else -> t.qualifiedName
      }
    }

    private fun deepEquals(t1: Throwable?, t2: Throwable?): Boolean {
      when {
        t1 === t2 -> return true
        t1 != null && t2 != null && t1.javaClass == t2.javaClass -> when {
          t1.message != t2.message -> return false
          !deepEquals(t1.cause, t2.cause) -> return false
          else -> {
            val sup1 = t1.suppressed
            val sup2 = t2.suppressed
            if (sup1.size == sup2.size) {
              if (sup1.indices.any { i -> !deepEquals(sup1[i], sup2[i]) }) {
                return false
              }
              val st1 = t1.stackTrace
              val st2 = t2.stackTrace
              return st1.size == st2.size && st1.indices.all { i -> st1[i] == st2[i] }
            } else {
              return false
            }
          }
        }
        else -> return false
      }
    }
  }
}