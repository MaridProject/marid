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
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.NoSuchElementException
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf

typealias Closer = (Runnable) -> Runnable

class Context private constructor(val name: String, val parent: Context?, closer: Closer) : AutoCloseable {

  private val queue = ConcurrentLinkedDeque<MoanHolder<*>>()
  private val typedMap = ConcurrentHashMap<KClassifier, ConcurrentLinkedQueue<MoanHolder<*>>>()
  private val namedMap = ConcurrentHashMap<String, MoanHolder<*>>()
  private val closeListeners = ConcurrentLinkedDeque<() -> Unit>()
  private val uid = UIDS.getAndUpdate { it.inc() }

  init {
    if (parent != null) {
      closeListeners.addFirst { close() }
    }
    CLEANABLES.computeIfAbsent(uid) { uid ->
      val name = this.name
      val queue = this.queue
      val typedMap = this.typedMap
      val namedMap = this.namedMap
      val closeListeners = this.closeListeners
      val closeTask = Runnable { close(uid, name, queue, typedMap, namedMap, closeListeners) }
      CLEANER.register(this, closer(closeTask))
    }
  }

  @Suppress("UNCHECKED_CAST")
  operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T {
    return by(property.returnType, property.name).value as T
  }

  fun by(type: KType, name: String?, optional: Boolean = false): MoanResult<Any?> {
    if (type.classifier == Seq::class) {
      val list = byType(type).toList()
      return if (list.isEmpty()) {
        if (optional) {
          MoanResult(null, true)
        } else {
          if (type.isMarkedNullable) {
            MoanResult(null)
          } else {
            MoanResult(Seq { emptySequence<Any?>().iterator() })
          }
        }
      } else {
        MoanResult(Seq { list.asSequence().map { it.moan }.iterator() })
      }
    } else {
      fun elseBranch(): MoanResult<Any?> {
        val list = byType(type).toList()
        return when (list.size) {
          0 -> {
            if (optional) {
              MoanResult(null, true)
            } else {
              if (type.isMarkedNullable) {
                MoanResult(null)
              } else {
                throw NoSuchElementException("No moan of type $type for $name")
              }
            }
          }
          1 -> MoanResult(list[0].moan)
          else ->
            throw MultipleBindingException(
              "Multiple moans of type $type for $name: ${list.map { it.name }}"
            )
        }
      }
      return if (name == null) {
        elseBranch()
      } else {
        val h = try {
          byName(name, type)
        } catch (e: NoSuchElementException) {
          null
        }
        if (h == null) {
          elseBranch()
        } else {
          MoanResult(h.moan)
        }
      }
    }
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

  internal fun <T, H : MoanHolder<T>> register(holder: H, scope: Scope? = null) {
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
        LOGGER.info("$name: Registered $holder")
        holder
      } else {
        throw DuplicatedMoanException(n)
      }
    }
  }

  fun <M : Module> init(module: M): M {
    try {
      LOGGER.info("$name: Initializing module $module")
      module.initialize()
      LOGGER.info("$name: Initialized module $module")
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

  override fun close() = close(uid, name, queue, typedMap, namedMap, closeListeners)

  override fun toString(): String = "Context($name)"

  companion object {

    private val CONTEXT_MAP = WeakHashMap<Any, Context>()
    private val CLEANER = Cleaner.create()
    private val UIDS = AtomicReference(BigInteger.ZERO)
    private val CLEANABLES = ConcurrentHashMap<BigInteger, Cleaner.Cleanable>(128, 0.5f)

    init {
      Scope.cleanOnShutdown("ContextCleaner", CLEANABLES)
    }

    operator fun invoke(name: String, parent: Context? = null, closer: Closer = { it }) = Context(name, parent, closer)

    fun <M : Module> Context.bind(module: (Context) -> M): M {
      val m = module(this)
      return init(m)
    }

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
      uid: BigInteger,
      name: String,
      queue: ConcurrentLinkedDeque<MoanHolder<*>>,
      typedMap: ConcurrentHashMap<KClassifier, ConcurrentLinkedQueue<MoanHolder<*>>>,
      namedMap: ConcurrentHashMap<String, MoanHolder<*>>,
      closeListeners: ConcurrentLinkedDeque<() -> Unit>
    ) {
      CLEANABLES.remove(uid)
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
          LOGGER.info("$name: Closing moan ${e.name}")
          e.close()
          LOGGER.info("$name: Closed moan ${e.name}")
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