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

import java.lang.reflect.Type
import java.lang.reflect.WildcardType as WT

data class WildcardType(val uBounds: Array<Type>, val lBounds: Array<Type>) : WT {

  override fun getUpperBounds(): Array<Type> = uBounds
  override fun getLowerBounds(): Array<Type> = lBounds

  override fun equals(other: Any?): Boolean = when {
    other === this -> true
    other is WT -> other.upperBounds contentEquals uBounds && other.lowerBounds contentEquals lBounds
    else -> false
  }

  override fun hashCode(): Int = lBounds.contentHashCode() xor uBounds.contentHashCode()

  override fun toString(): String {
    val ep = uBounds.takeIf { it.isNotEmpty() }?.run { joinToString(" & ", " extends") { it.typeName } } ?: ""
    val sp = lBounds.takeIf { it.isNotEmpty() }?.run { joinToString(" & ", " super ") { it.typeName } } ?: ""
    return "?$ep$sp"
  }
}