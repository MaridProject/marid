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

import java.lang.ref.Cleaner
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.NoSuchElementException
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf

typealias Closer = (Runnable) -> Runnable

class Context private constructor(val name: String, val parent: Context?, closer: Closer) : AutoCloseable {

  private val queue = ConcurrentLinkedDeque<MoanHolder<*>>()
  private val typedMap = ConcurrentHashMap<KClassifier, ConcurrentLinkedQueue<MoanHolder<*>>>()
  private val namedMap = ConcurrentHashMap<String, MoanHolder<*>>()
  private val closeListeners = ConcurrentLinkedDeque<() -> Unit>()
  private val cleanRef: Cleaner.Cleanable

  init {
    if (parent != null) {
      closeListeners.addFirst { close() }
    }
    val name = this.name
    val queue = this.queue
    val typedMap = this.typedMap
    val namedMap = this.namedMap
    val closeListeners = this.closeListeners
    val closeTask = Runnable { close(name, queue, typedMap, namedMap, closeListeners) }
    cleanRef = CLEANER.register(this, closer(closeTask))
  }

  inline fun <reified T> singleton(name: String, noinline factory: Context.() -> T): MemoizedMoanHolder<T> {
    val t = object : MoanHolderTypeResolver<T>() {}
    val h = if (T::class.isSubclassOf(ContextAware::class)) {
      SingletonMoanHolder(name, t.type) { withContext(this, factory(this)) }
    } else {
      SingletonMoanHolder(name, t.type) { factory(this) }
    }
    register(h)
    return h
  }

  inline fun <reified T> prototype(name: String, noinline factory: Context.() -> T): StatelessMoanHolder<T> {
    val t = object : MoanHolderTypeResolver<T>() {}
    val h = if (T::class.isSubclassOf(ContextAware::class)) {
      PrototypeMoanHolder(name, t.type) { withContext(this, factory(this)) }
    } else {
      PrototypeMoanHolder(name, t.type) { factory(this) }
    }
    register(h)
    return h
  }

  inline fun <reified T> scoped(name: String, scope: Scope, noinline factory: Context.() -> T): MemoizedMoanHolder<T> {
    val t = object : MoanHolderTypeResolver<T>() {}
    val h = if (T::class.isSubclassOf(ContextAware::class)) {
      ScopedMoanHolder(name, t.type) { withContext(this, factory(this)) }
    } else {
      ScopedMoanHolder(name, t.type) { factory(this) }
    }
    register(h, scope)
    return h
  }

  inline fun <reified T> by(): T {
    val t = object : MoanHolderTypeResolver<T>() {}
    return (byType(t.type).firstOrNull()?.moan as T?) ?: throw NoSuchElementException(t.toString())
  }

  inline fun <reified T> seq(): Seq<T> {
    val t = object : MoanHolderTypeResolver<T>() {}
    val s = byType(t.type).map { it.moan as T }
    return Seq { s.iterator() }
  }

  inline fun <reified T> by(name: String): T {
    val t = object : MoanHolderTypeResolver<T>() {}
    return byName(name, t.type).moan as T
  }

  @Suppress("UNCHECKED_CAST")
  operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T {
    return byName(property.name, property.returnType).moan as T
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
              else -> {
                try {
                  parent.byName(name, type)
                } catch (e: NoSuchElementException) {
                  val x = ClassCastException("Moan $name of ${m.type} cannot be cast to $type")
                  x.initCause(e)
                  throw x
                } catch (e: ClassCastException) {
                  val x = ClassCastException("Moan $name of ${m.type} cannot be cast to $type")
                  x.initCause(e)
                  throw x
                }
              }
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

  inline fun <reified M : Module> init(module: M): M {
    try {
      module.initialize()
    } catch (e: Throwable) {
      try {
        close()
      } catch (x: Throwable) {
        e.addSuppressed(x)
      }
      throw e
    }
    return module
  }

  override fun close() = cleanRef.clean()

  override fun toString(): String = "Context($name)"

  companion object {

    private val CONTEXT_MAP = WeakHashMap<Any, Context>()
    private val CLEANER = Cleaner.create()

    operator fun invoke(name: String, parent: Context? = null, closer: Closer = { it }) = Context(name, parent, closer)

    fun <T> withContext(context: Context, t: T): T {
      synchronized(CONTEXT_MAP) {
        CONTEXT_MAP[t] = context
      }
      return t
    }

    fun contextFor(obj: ContextAware): Context? = synchronized(CONTEXT_MAP) { CONTEXT_MAP[obj] }

    fun <T, H : MoanHolder<T>> H.withInitHook(hook: (T) -> Unit): H {
      postConstructHooks.add(hook)
      return this
    }

    private fun close(
      name: String,
      queue: ConcurrentLinkedDeque<MoanHolder<*>>,
      typedMap: ConcurrentHashMap<KClassifier, ConcurrentLinkedQueue<MoanHolder<*>>>,
      namedMap: ConcurrentHashMap<String, MoanHolder<*>>,
      closeListeners: ConcurrentLinkedDeque<() -> Unit>
    ) {
      typedMap.clear()
      namedMap.clear()
      val exception = ContextCloseException(name)
      closeListeners.removeIf {
        try {
          it()
        } catch (e: Throwable) {
          exception.addSuppressed(e)
        }
        true
      }
      val it = queue.descendingIterator()
      while (it.hasNext()) {
        val e = it.next()
        try {
          e.close()
        } catch (x: Throwable) {
          exception.addSuppressed(x)
        } finally {
          it.remove()
        }
      }
      if (exception.suppressed.isNotEmpty()) {
        throw exception
      }
    }
  }
}