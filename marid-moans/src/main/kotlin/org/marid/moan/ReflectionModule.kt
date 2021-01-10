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

import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSupertypeOf

abstract class ReflectionModule(context: Context) : Module(context) {

  private fun args(callable: KCallable<*>): Map<KParameter, Any?> {
    val parameters = callable.parameters
    val args = mutableMapOf<KParameter, Any?>()
    for (p in parameters) {
      val type = p.type
      if (type.classifier == Seq::class) {
        val list = context.byType(type).toList()
        if (list.isEmpty()) {
          if (!p.isOptional) {
            if (type.isMarkedNullable) {
              args[p] = null
            } else {
              args[p] = Seq { emptySequence<Any?>().iterator() }
            }
          }
        } else {
          args[p] = Seq { list.asSequence().map { it.moan }.iterator() }
        }
      } else {
        val name = p.name
        fun elseBranch() {
          val list = context.byType(type).toList()
          when (list.size) {
            0 -> {
              if (!p.isOptional) {
                if (type.isMarkedNullable) {
                  args[p] = null
                } else {
                  throw NoSuchElementException("No moan of type $type for param $p of $callable")
                }
              }
            }
            1 -> args[p] = list[0].moan
            else ->
              throw MultipleBindingException(
                "Multiple moans of type $type for param $p of $callable: ${list.map { it.name }}"
              )
          }
        }
        if (name == null) {
          elseBranch()
        } else {
          val h = try {
            context.byName(name, type)
          } catch (e: NoSuchElementException) {
            null
          }
          if (h == null) {
            elseBranch()
          } else {
            args[p] = h.moan
          }
        }
      }
    }
    return args
  }

  override val init: Context.() -> Unit
    get() = {
      for (callable in this::class.members) {
        val singleton = callable.findAnnotation<Singleton>()
        if (singleton != null) {
          val name = singleton.name.takeIf { it.isNotBlank() } ?: callable.name
          val type = callable.returnType
          val h = if (ContextAware::class.createType().isSupertypeOf(type)) {
            SingletonMoanHolder(name, type) { Context.withContext(context, callable.callBy(args(callable))) }
          } else {
            SingletonMoanHolder(name, type) { callable.callBy(args(callable)) }
          }
          context.register(h)
          continue
        }
        val prototype = callable.findAnnotation<Prototype>()
        if (prototype != null) {
          val name = prototype.name.takeIf { it.isNotBlank() } ?: callable.name
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

  protected companion object {
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FUNCTION)
    annotation class Singleton(val name: String = "")

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FUNCTION)
    annotation class Prototype(val name: String = "")
  }
}