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

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.isSubtypeOf

sealed class Type {
  abstract fun supertypes(): Set<Type>
  abstract fun isSubtypeOf(type: Type): Boolean
  fun isSupertypeOf(type: Type): Boolean = type.isSubtypeOf(this)
  abstract override fun toString(): String
  abstract override fun equals(other: Any?): Boolean
  abstract override fun hashCode(): Int
}

class SimpleType(val type: KType) : Type() {

  override fun supertypes(): Set<Type> = when (val c = type.classifier) {
    is KClass<*> -> {
      val args = type.arguments
      if (args.isEmpty()) {
        setOf(this) + c.allSupertypes.map(::SimpleType)
      } else {
        val formalArgs = c.typeParameters
        if (formalArgs.size == args.size) {
          val map = mapOf(*formalArgs.zip(args).toTypedArray())
          setOf(this) + c.allSupertypes.mapNotNull { it.substituteFrom(map)?.let(::SimpleType) }.toSet()
        } else {
          setOf(this) // outer classes aren't supported
        }
      }
    }
    else -> setOf(this)
  }

  override fun isSubtypeOf(type: Type): Boolean = when (type) {
    is SimpleType -> this.type.isSubtypeOf(type.type)
    is IntersectionType -> type.types.all { isSubtypeOf(it) }
  }

  override fun toString(): String = type.toString()

  override fun equals(other: Any?): Boolean = when {
    other === this -> true
    other is SimpleType -> this.type == other.type
    other is IntersectionType -> other.types.size == 1 && other.types[0] == this
    else -> false
  }

  override fun hashCode(): Int = type.hashCode()
}

class IntersectionType(val types: List<Type>) : Type() {

  override fun supertypes(): Set<Type> = types.map(Type::supertypes).reduce { a, b -> a + b }

  override fun isSubtypeOf(type: Type): Boolean = when (type) {
    is SimpleType -> types.any { it.isSubtypeOf(type) }
    is IntersectionType -> types.any { t -> type.types.all { t.isSubtypeOf(it) } }
  }

  override fun toString(): String = types.joinToString(",", "[", "]")

  override fun equals(other: Any?): Boolean = when {
    other === this -> true
    other is SimpleType -> types.size == 1 && types[0] == other
    other is IntersectionType -> {
      if (types.size != other.types.size) {
        false
      } else {
        other.types.all { e -> types.any { e == it } }
      }
    }
    else -> false
  }

  override fun hashCode(): Int = types.fold(0) { acc, type -> acc + type.hashCode() }
}