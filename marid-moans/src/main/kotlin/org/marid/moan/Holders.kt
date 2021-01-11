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

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KType

abstract class MoanHolder<T>(
  val contextName: String,
  val name: String,
  val type: KType,
  protected val moanFactory: () -> T
) : AutoCloseable {

  internal val postConstructHooks = ConcurrentLinkedQueue<(T) -> Unit>()
  private val closeListeners = ConcurrentLinkedQueue<Runnable>()

  abstract val moan: T

  fun addCloseListener(listener: Runnable) {
    closeListeners += listener
  }

  override fun close() {
    val exception = IllegalStateException("Unable to call close listeners")
    closeListeners.removeIf {
      try {
        it.run()
      } catch (e: Throwable) {
        exception.addSuppressed(e)
      }
      true
    }
    if (exception.suppressed.isNotEmpty()) {
      throw exception
    }
  }

  override fun toString(): String = "${javaClass.simpleName}($name: $type)"
}

abstract class DestroyAwareMoanHolder<T>(
  contextName: String,
  name: String,
  type: KType,
  f: () -> T
) : MoanHolder<T>(contextName, name, type, f) {

  @Volatile private var closed = false
  protected val preDestroyHooks = ConcurrentLinkedDeque<(T) -> Unit>()
  protected abstract val currentMoan: T?

  override fun close() {
    if (closed) {
      return
    } else {
      synchronized(this) {
        if (closed) {
          return
        }
        when (val m = currentMoan) {
          null -> return
          else -> {
            val e = MoanDestructionException(name)
            try {
              super.close()
            } catch (x: Throwable) {
              e.addSuppressed(x)
            }
            for (h in preDestroyHooks.descendingIterator()) {
              try {
                h(m)
              } catch (x: Throwable) {
                e.addSuppressed(x)
              }
            }
            if (e.suppressed.isNotEmpty()) {
              throw e
            }
          }
        }
      }
    }
  }

  companion object {
    fun <T, H : DestroyAwareMoanHolder<T>> H.withDestroyHook(hook: (T) -> Unit): H {
      preDestroyHooks.add(hook)
      return this
    }
  }
}

abstract class MemoizedMoanHolder<T>(
  contextName: String,
  name: String,
  type: KType,
  f: () -> T
) : DestroyAwareMoanHolder<T>(contextName, name, type, f) {

  @Volatile override var currentMoan: T? = null

  override val moan: T
    get() {
      if (currentMoan == null) {
        synchronized(this) {
          if (currentMoan == null) {
            LOGGER.info("$contextName: Initializing moan $name of $type")
            val m = moanFactory()
            currentMoan = m
            if (m is Moan) {
              preDestroyHooks.addFirst { m.destroy() }
              postConstructHooks.add { m.init() }
            }
            if (m is AutoCloseable) {
              preDestroyHooks.addFirst { m.close() }
            }
            postConstructHooks.removeIf { h ->
              try {
                h(m)
              } catch (e: Throwable) {
                val ex = MoanCreationException(name, e)
                try {
                  close()
                } catch (x: Throwable) {
                  ex.addSuppressed(x)
                }
                throw ex
              }
              true
            }
            LOGGER.info("$contextName: Initialized moan $name of $type")
          }
        }
      }
      return currentMoan!!
    }
}

abstract class StatelessMoanHolder<T>(
  contextName: String,
  name: String,
  type: KType,
  f: () -> T
) : MoanHolder<T>(contextName, name, type, f) {
  override val moan: T
    get() {
      val m = moanFactory()
      if (m is Moan) {
        m.init()
      }
      postConstructHooks.removeIf { h ->
        h(m)
        true
      }
      return m
    }
}

class SingletonMoanHolder<T>(
  contextName: String,
  name: String,
  type: KType,
  f: () -> T
) : MemoizedMoanHolder<T>(contextName, name, type, f)

class PrototypeMoanHolder<T>(
  contextName: String,
  name: String,
  type: KType,
  f: () -> T
) : StatelessMoanHolder<T>(contextName, name, type, f)

class ScopedMoanHolder<T>(
  contextName: String,
  name: String,
  type: KType,
  f: () -> T
) : MemoizedMoanHolder<T>(contextName, name, type, f)