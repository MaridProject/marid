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

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSupertypeOf

abstract class ReflectionModule(context: Context) : Module(context) {

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

  override val init: Context.() -> Unit
    get() = {
      for (callable in this::class.members) {
        init(callable, callable) { callable.name }
      }
    }

  fun init(vararg classes: KClass<*>) {
    for (cl in classes) {
      for (c in cl.constructors) {
        init(c, cl) { cl.qualifiedName ?: cl.toString() }
      }
    }
  }

  private fun init(callable: KCallable<*>, annotatedElement: KAnnotatedElement, defaultName: () -> String) {
    val singleton = annotatedElement.findAnnotation<Singleton>()
    if (singleton != null) {
      val name = singleton.name.takeIf { it.isNotBlank() } ?: defaultName()
      val type = callable.returnType
      val h = if (ContextAware::class.createType().isSupertypeOf(type)) {
        SingletonMoanHolder(name, type) { Context.withContext(context, callable.callBy(args(callable))) }
      } else {
        SingletonMoanHolder(name, type) { callable.callBy(args(callable)) }
      }
      context.register(h)
      return
    }
    val prototype = annotatedElement.findAnnotation<Prototype>()
    if (prototype != null) {
      val name = prototype.name.takeIf { it.isNotBlank() } ?: defaultName()
      val type = callable.returnType
      val h = if (ContextAware::class.createType().isSupertypeOf(type)) {
        PrototypeMoanHolder(name, type) { Context.withContext(context, callable.callBy(args(callable))) }
      } else {
        PrototypeMoanHolder(name, type) { callable.callBy(args(callable)) }
      }
      context.register(h)
    }
  }
}