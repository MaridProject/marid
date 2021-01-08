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

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.NoSuchElementException
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf

class Context(val name: String, val parent: Context? = null) {

  private val queue = ConcurrentLinkedDeque<MoanHolder<*>>()
  private val typedMap = ConcurrentHashMap<KClassifier, ConcurrentLinkedQueue<MoanHolder<*>>>()
  private val namedMap = ConcurrentHashMap<String, MoanHolder<*>>()

  inline fun <reified T> singleton(name: String, noinline factory: Context.() -> T): MemoizedMoanHolder<T> {
    val t = object : MoanHolderTypeResolver<T>() {}
    val h = if (T::class.isSubclassOf(ContextAware::class)) {
      ContextSingletonMoanHolder(name, t.type, this) { factory(this) }
    } else {
      SingletonMoanHolder(name, t.type) { factory(this) }
    }
    register(h)
    return h
  }

  inline fun <reified T> prototype(name: String, noinline factory: Context.() -> T): StatelessMoanHolder<T> {
    val t = object : MoanHolderTypeResolver<T>() {}
    val h = if (T::class.isSubclassOf(ContextAware::class)) {
      ContextPrototypeMoanHolder(name, t.type, this) { factory(this) }
    } else {
      PrototypeMoanHolder(name, t.type) { factory(this) }
    }
    register(h)
    return h
  }

  inline fun <reified T> scoped(name: String, scope: Scope, noinline factory: Context.() -> T): MemoizedMoanHolder<T> {
    val t = object : MoanHolderTypeResolver<T>() {}
    val h = if (T::class.isSubclassOf(ContextAware::class)) {
      ContextScopedMoanHolder(name, t.type, this) { factory(this) }
    } else {
      ScopedMoanHolder(name, t.type) { factory(this) }
    }
    register(h, scope)
    return h
  }

  inline fun <reified T> byType(): T {
    val t = object : MoanHolderTypeResolver<T>() {}
    return (byType(t.type).firstOrNull()?.moan as T?) ?: throw NoSuchElementException(t.toString())
  }

  inline fun <reified T> seqByType(): Sequence<T> {
    val t = object : MoanHolderTypeResolver<T>() {}
    return byType(t.type).map { it.moan as T }
  }

  inline fun <reified T> byName(name: String): T {
    val t = object : MoanHolderTypeResolver<T>() {}
    return byName(name, t.type).moan as T
  }

  fun byType(type: KType): Sequence<MoanHolder<*>> {
    val c = when (val c = type.classifier) {
      is KClassifier -> typedMap[c]?.let(Collection<MoanHolder<*>>::asSequence) ?: emptySequence()
      else -> queue.asSequence().filter { it.type.isSubtypeOf(type) }
    }
    return when (parent) {
      null -> c
      else -> c + parent.byType(type)
    }
  }

  fun byName(name: String, type: KType): MoanHolder<*> {
    return when (val m = namedMap[name]) {
      null ->
        when (parent) {
          null -> throw NoSuchElementException(name)
          else -> parent.byName(name, type)
        }
      else ->
        when {
          m.type.isSubtypeOf(type) -> m
          else ->
            when (parent) {
              null -> throw ClassCastException("Moan $name of ${m.type} cannot be cast to $type")
              else -> parent.byName(name, type)
            }
        }
    }
  }

  fun <T, H : MoanHolder<T>> register(holder: H, scope: Scope? = null) {
    namedMap.compute(name) { n, old ->
      if (old == null) {
        if (scope != null && holder is ScopedMoanHolder<*>) {
          scope.add(holder)
        }
        queue += holder
        val type = holder.type
        when (val c = type.classifier) {
          is KClassifier -> typedMap.computeIfAbsent(c) { ConcurrentLinkedQueue<MoanHolder<*>>() } += holder
        }
        holder
      } else {
        throw DuplicatedMoanException(n)
      }
    }
  }

  override fun toString(): String = "Context($name)"
}