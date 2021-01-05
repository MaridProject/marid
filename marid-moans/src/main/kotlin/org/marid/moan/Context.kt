package org.marid.moan

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClassifier
import kotlin.reflect.KType

class Context(val name: String, val parent: Context?) {

  private val queue = ConcurrentLinkedDeque<MoanHolder<*>>()
  private val typedMap = ConcurrentHashMap<KClassifier, ConcurrentLinkedQueue<*>>()
  private val namedMap = ConcurrentHashMap<String, MoanHolder<*>>()

  inline fun <reified T> singleton(name: String, noinline factory: () -> T): SingletonMoanHolder<T> {
    val h = object : SingletonMoanHolder<T>(name, factory) {}
    register(h)
    return h
  }

  inline fun <reified T> bind(name: String, noinline factory: () -> T): SingletonMoanHolder<T> = singleton(name, factory)

  inline fun <reified T> prototype(name: String, noinline factory: () -> T): PrototypeMoanHolder<T> {
    val h = object : PrototypeMoanHolder<T>(name, factory) {}
    register(h)
    return h
  }

  fun <T> register(holder: MoanHolder<T>) {
    namedMap.compute(name) { n, old ->
      if (old == null) {
        holder
      } else {
        throw DuplicatedMoanException(n)
      }
    }
  }

  companion object {
    abstract class MoanHolder<T>(val name: String, protected val moanFactory: () -> T) {

      protected val postConstructHooks = ConcurrentLinkedQueue<(T) -> Unit>()

      abstract val moan: T

      val type: KType
        get() = this::class.supertypes
          .first { it.classifier == MoanHolder::class }
          .arguments[0]
          .type!!

      companion object {
        fun <T, H : MoanHolder<T>> H.withInitHook(hook: (T) -> Unit): H {
          postConstructHooks.add(hook)
          return this
        }
      }
    }

    abstract class DestroyAwareMoanHolder<T>(name: String, f: () -> T) : MoanHolder<T>(name, f), AutoCloseable {

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

    abstract class MemoizedMoanHolder<T>(name: String, f: () -> T) : DestroyAwareMoanHolder<T>(name, f) {

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

    abstract class SingletonMoanHolder<T>(name: String, f: () -> T) : MemoizedMoanHolder<T>(name, f)

    abstract class PrototypeMoanHolder<T>(name: String, f: () -> T) : MoanHolder<T>(name, f) {
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

    abstract class ScopedMoanHolder<T>(name: String, f: () -> T) : MemoizedMoanHolder<T>(name, f)
  }
}