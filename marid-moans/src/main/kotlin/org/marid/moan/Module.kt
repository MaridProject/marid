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

import org.marid.moan.Context.Companion.args
import java.util.logging.Level
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

abstract class Module(val context: Context) {

  open fun dependsOn(): Sequence<Module> = emptySequence()

  abstract fun init()

  internal fun initialize(dependencyMapper: DependencyMapper) {
    val logger = context.path.asLogger
    try {
      logger.log(Level.INFO, "Initializing $this")
      for (dep in dependsOn().flatMap(dependencyMapper)) {
        dep.initialize(dependencyMapper)
      }
      init()
      logger.log(Level.INFO, "Initialized $this")
    } catch (e: Throwable) {
      throw ModuleInitializationException(toString(), e)
    }
  }

  fun singleton(callable: KCallable<*>, name: String = callable.safeName): SingletonMoanHolder<*> {
    val type = callable.returnType
    val h = SingletonMoanHolder(context, name, type) { callable.callBy(context.args(callable)) }
    context.register(h)
    return h
  }

  fun prototype(callable: KCallable<*>, name: String = callable.safeName): PrototypeMoanHolder<*> {
    val type = callable.returnType
    val h = PrototypeMoanHolder(context, name, type) { callable.callBy(context.args(callable)) }
    context.register(h)
    return h
  }

  fun singleton(callable: KCallable<*>, scope: Scope, name: String = callable.safeName): ScopedMoanHolder<*> {
    val type = callable.returnType
    val h = ScopedMoanHolder(context, name, type) { callable.callBy(context.args(callable)) }
    context.register(h, scope)
    return h
  }

  override fun toString(): String = javaClass.name

  internal companion object {
    internal val KCallable<*>.safeName
      get() = when (name) {
        "<init>" ->
          when (val c = returnType.classifier) {
            is KClass<*> -> c.java.name
            else -> name
          }
        else -> name
      }
  }
}