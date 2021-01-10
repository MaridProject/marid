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

abstract class Module(val context: Context) {

  inline fun <reified M : Module> init(module: M): M {
    module.initialize()
    return module
  }

  open val preInit: Context.() -> Unit = {}
  abstract val init: Context.() -> Unit
  open val postInit: Context.() -> Unit = {}

  fun initialize() {
    try {
      preInit(context)
      init(context)
      postInit(context)
    } catch (e: Throwable) {
      throw ModuleInitializationException(toString(), e)
    }
  }

  override fun toString(): String = "${context.name}.${javaClass.name}"
}