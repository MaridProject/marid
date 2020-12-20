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
import java.lang.reflect.ParameterizedType as PT

data class ParameterizedType(val owner: Type?, val raw: Class<*>, val args: Array<Type>) : PT {

  override fun getActualTypeArguments(): Array<Type> = args
  override fun getRawType(): Type = raw
  override fun getOwnerType(): Type? = owner

  override fun equals(other: Any?): Boolean = when {
    other === this -> true
    other is PT -> other.ownerType == owner && other.rawType == raw && other.actualTypeArguments contentEquals args
    else -> false
  }

  override fun hashCode(): Int = args.contentHashCode() xor owner.hashCode() xor raw.hashCode()

  override fun toString(): String {
    return owner?.typeName?.let { "$it." } + raw.name + args.joinToString(",", "<", ">") { it.typeName }
  }
}