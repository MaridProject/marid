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

interface ContextAware {

  val context: Context get() = Context.contextFor(this) ?: throw ContextBoundException(this::class)

  operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T = context.getValue(thisRef, property)

  companion object {
    inline fun <reified T> ContextAware.byType(): T = context.byType()
    inline fun <reified T> ContextAware.seqByType(): Sequence<T> = context.seqByType()
    inline fun <reified T> ContextAware.byName(name: String): T = context.byName<T>(name)
  }
}