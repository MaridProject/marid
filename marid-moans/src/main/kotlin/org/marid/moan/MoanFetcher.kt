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
package org.marid.moan

import kotlin.reflect.KProperty
import kotlin.reflect.KType

interface MoanFetcher {

  @Suppress("UNCHECKED_CAST")
  operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T {
    return by(property.returnType, property.name, false).value as T
  }

  fun by(type: KType, name: String?, optional: Boolean): MoanResult<Any?>
}