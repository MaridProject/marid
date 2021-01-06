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
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.reflect.KType

abstract class MoanHolder<T>(val name: String, val type: KType, protected val moanFactory: () -> T) : AutoCloseable {

  protected val postConstructHooks = ConcurrentLinkedQueue<(T) -> Unit>()
  private val closeListeners = ConcurrentLinkedQueue<Runnable>()

  abstract val moan: T

  fun addCloseListener(listener: Runnable) {
    closeListeners += listener
  }

  override fun close() {
    closeListeners.removeIf {
      try {
        it.run()
      } catch (e: Throwable) {
        try {
          val logger = Logger.getLogger(name)
          val record = LogRecord(Level.WARNING, "Unable to call close handler")
          record.thrown = e
          record.sourceClassName = null
          record.loggerName = name
          logger.log(record)
        } catch (_: Throwable) {
          // impossible
        }
      }
      true
    }
  }

  companion object {
    fun <T, H : MoanHolder<T>> H.withInitHook(hook: (T) -> Unit): H {
      postConstructHooks.add(hook)
      return this
    }
  }
}

abstract class DestroyAwareMoanHolder<T>(name: String, type: KType, f: () -> T) : MoanHolder<T>(name, type, f) {

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
            super.close()
            val e = MoanDestructionException(name)
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

abstract class MemoizedMoanHolder<T>(name: String, type: KType, f: () -> T) : DestroyAwareMoanHolder<T>(name, type, f) {

  @Volatile override var currentMoan: T? = null

  override val moan: T
    get() {
      if (currentMoan == null) {
        synchronized(this) {
          if (currentMoan == null) {
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
          }
        }
      }
      return currentMoan!!
    }
}

class SingletonMoanHolder<T>(name: String, type: KType, f: () -> T) : MemoizedMoanHolder<T>(name, type, f)

class PrototypeMoanHolder<T>(name: String, type: KType, f: () -> T) : MoanHolder<T>(name, type, f) {
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

class ScopedMoanHolder<T>(name: String, type: KType, f: () -> T) : MemoizedMoanHolder<T>(name, type, f)

abstract class MoanHolderTypeResolver<T> {
  val type: KType
    get() = this::class.supertypes
      .first { it.classifier == MoanHolderTypeResolver::class }
      .arguments[0]
      .type!!
}