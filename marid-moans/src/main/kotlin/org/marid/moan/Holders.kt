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
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level.INFO
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

abstract class MoanHolder<T>(val context: Context, val name: String, val type: KType, val factory: () -> T) {

  internal val postConstructHooks = ConcurrentLinkedQueue<(T) -> Unit>()
  private val closeListeners = ConcurrentLinkedQueue<Runnable>()

  abstract val moan: T

  fun addCloseListener(listener: Runnable) {
    closeListeners += listener
  }

  protected fun init(instance: T) {
    val type = instance!!::class
    when (instance) {
      is Initializable -> {
        for (fn in type.memberFunctions) {
          if (fn.findAnnotation<Init>() != null) {
            postConstructHooks += { fn.callBy(context.args(fn)) }
          }
        }
      }
    }
    postConstructHooks.removeIf { h ->
      try {
        h(instance)
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

  open fun close() {
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

abstract class DestroyAwareMoanHolder<T>(context: Context, name: String, type: KType, f: () -> T) :
  MoanHolder<T>(context, name, type, f) {

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

abstract class MemoizedMoanHolder<T>(context: Context, name: String, type: KType, f: () -> T) :
  DestroyAwareMoanHolder<T>(context, name, type, f) {

  @Volatile override var currentMoan: T? = null

  override val moan: T
    get() {
      if (currentMoan == null) {
        synchronized(this) {
          if (currentMoan == null) {
            val logger = context.path.asLogger
            logger.log(INFO, "Initializing moan $name of $type")
            val m = factory()
            currentMoan = m
            if (m is Moan) {
              preDestroyHooks.addFirst { m.destroy() }
              postConstructHooks += { m.init() }
            }
            if (m is AutoCloseable) {
              preDestroyHooks.addFirst { m.close() }
            }
            init(m)
            logger.log(INFO, "Initialized moan $name of $type")
          }
        }
      }
      return currentMoan!!
    }
}

abstract class StatelessMoanHolder<T>(context: Context, name: String, type: KType, f: () -> T) :
  MoanHolder<T>(context, name, type, f) {
  override val moan: T
    get() {
      val m = factory()
      if (m is Moan) {
        postConstructHooks += { m.init() }
      }
      init(m)
      return m
    }
}

class SingletonMoanHolder<T>(context: Context, name: String, type: KType, f: () -> T) :
  MemoizedMoanHolder<T>(context, name, type, f)

class PrototypeMoanHolder<T>(context: Context, name: String, type: KType, f: () -> T) :
  StatelessMoanHolder<T>(context, name, type, f)

class ScopedMoanHolder<T>(context: Context, name: String, type: KType, f: () -> T) :
  MemoizedMoanHolder<T>(context, name, type, f)