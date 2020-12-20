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

import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type

data class ArrayType(val componentType: Type) : GenericArrayType {

  override fun getGenericComponentType(): Type = componentType

  override fun equals(other: Any?): Boolean = when {
    other === this -> true
    other is GenericArrayType -> other.genericComponentType == componentType
    else -> false
  }

  override fun hashCode(): Int = componentType.hashCode()

  override fun toString(): String = componentType.typeName + "[]"
}