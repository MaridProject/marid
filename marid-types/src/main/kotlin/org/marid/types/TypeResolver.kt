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
package org.marid.types

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.stream.Collectors
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVariance
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

object TypeResolver {

  private val COMMON_CLASS_PREFIXES = arrayOf(
    "java.",
    "kotlin.",
    "scala."
  )

  fun compareClasses(c1: KClass<*>, c2: KClass<*>): Int = when {
    c1 == c2 -> 0
    c1.isSubclassOf(c2) -> -1
    c1.isSuperclassOf(c2) -> 1
    else -> c1.jvmName.compareTo(c2.jvmName)
  }

  fun compareTypes(t1: KType, t2: KType): Int = when {
    t1 == t2 -> 0
    t1.isSubtypeOf(t2) -> -1
    t1.isSupertypeOf(t2) -> 1
    else -> t1.toString().compareTo(t2.toString())
  }

  @ExperimentalStdlibApi
  fun foldTypes(vararg types: KType): KType {
    val ts = ConcurrentLinkedQueue(types.asList())
    while (true) {
      val result = ts.removeIf { t -> ts.parallelStream().anyMatch { t !== it && it.isSubtypeOf(t) } }
      if (!result) {
        break
      }
    }
    ts.removeIf { t -> t.classifier == Any::class }
    val customTypes = ts.parallelStream()
      .filter { t -> t.typeName.let { name -> COMMON_CLASS_PREFIXES.none { name.startsWith(it) } } }
      .sorted(Comparator.comparingInt { t ->
        when (val c = t.classifier) {
          is KClass<*> -> c.superclasses.size
          else -> 0
        }
      })
      .collect(Collectors.toUnmodifiableList())
    if (customTypes.isNotEmpty()) {
      return customTypes.lastOrNull()!!
    }
    return typeOf<Any>()
  }
}

val KType.typeName: String
  get() =
    when (val c = classifier) {
      is KClass<*> ->
        if (arguments.isEmpty()) {
          c.qualifiedName ?: "*"
        } else {
          arguments.joinToString(",", c.qualifiedName + "<", ">") {
            when (it.variance) {
              KVariance.IN -> "-" + (it.type?.typeName ?: "*")
              KVariance.OUT -> "+" + (it.type?.typeName ?: "*")
              else -> it.type?.typeName ?: "*"
            }
          }
        }
      else -> "*"
    }