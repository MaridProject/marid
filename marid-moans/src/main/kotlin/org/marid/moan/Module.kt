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

import java.util.logging.Level
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf

abstract class Module(val context: Context) {

  open fun dependsOn(): List<Module> = listOf()

  abstract fun init()

  fun initialize() {
    val logger = context.path.asLogger
    try {
      logger.log(Level.INFO, "Initializing $this")
      for (dep in dependsOn()) {
        dep.initialize()
      }
      init()
      logger.log(Level.INFO, "Initialized $this")
    } catch (e: Throwable) {
      throw ModuleInitializationException(toString(), e)
    }
  }

  private fun args(callable: KCallable<*>): Map<KParameter, Any?> {
    val parameters = callable.parameters
    val args = mutableMapOf<KParameter, Any?>()
    for (p in parameters) {
      when (p.kind) {
        KParameter.Kind.INSTANCE -> args[p] = this
        KParameter.Kind.EXTENSION_RECEIVER -> throw IllegalStateException()
        KParameter.Kind.VALUE -> {
          val result = context.by(p.type, p.name, p.isOptional)
          if (!result.empty) {
            args[p] = result.value
          }
        }
      }
    }
    return args
  }

  fun singleton(callable: KCallable<*>, name: String = callable.safeName) {
    val type = callable.returnType
    val h = if (ContextAware::class.createType().isSupertypeOf(type)) {
      SingletonMoanHolder(context, name, type) { Context.withContext(context, callable.callBy(args(callable))) }
    } else {
      SingletonMoanHolder(context, name, type) { callable.callBy(args(callable)) }
    }
    context.register(h)
  }

  fun prototype(callable: KCallable<*>, name: String = callable.safeName) {
    val type = callable.returnType
    val h = if (ContextAware::class.createType().isSupertypeOf(type)) {
      PrototypeMoanHolder(context, name, type) { Context.withContext(context, callable.callBy(args(callable))) }
    } else {
      PrototypeMoanHolder(context, name, type) { callable.callBy(args(callable)) }
    }
    context.register(h)
  }

  fun singleton(callable: KCallable<*>, scope: Scope, name: String = callable.safeName) {
    val type = callable.returnType
    val h = if (ContextAware::class.createType().isSupertypeOf(type)) {
      ScopedMoanHolder(context, name, type) { Context.withContext(context, callable.callBy(args(callable))) }
    } else {
      ScopedMoanHolder(context, name, type) { callable.callBy(args(callable)) }
    }
    context.register(h, scope)
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