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
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

val KType.isGround: Boolean
  get() = when (classifier) {
    is KClass<*> -> arguments.all { t ->
      when (val e = t.type) {
        is KType -> e.isGround
        else -> true // star projection
      }
    }
    else -> false
  }

fun KType.substituteFrom(map: Map<KTypeParameter, KTypeProjection>): KType? {
  if (isGround) {
    return this
  } else {
    when (val c = classifier) {
      is KTypeParameter -> return map[c]?.type
      is KClass<*> -> {
        val args = arguments
        if (args.isEmpty()) {
          return this
        } else {
          val list = mutableListOf<KTypeProjection>()
          for (p in args) {
            when (val t = p.type) {
              null -> list += p
              else -> {
                when (val cc = t.classifier) {
                  is KTypeParameter -> {
                    val pp = map[cc]
                    if (pp == null) {
                      return null
                    } else {
                      list += pp
                    }
                  }
                  is KClass<*> -> {
                    val tt = t.substituteFrom(map)
                    if (tt == null) {
                      return null
                    } else {
                      list += KTypeProjection(p.variance, t.substituteFrom(map))
                    }
                  }
                  else -> return null
                }
              }
            }
          }
          return c.createType(list, isMarkedNullable, annotations)
        }
      }
      else -> return null
    }
  }
}