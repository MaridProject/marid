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
import java.util.logging.Level.INFO
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubtypeOf

typealias Closer = (Runnable) -> Runnable

class Context private constructor(
  val name: String,
  val parent: Context?,
  closer: Closer,
  val properties: Properties
): MoanFetcher, AutoCloseable {

  private val queue = ConcurrentLinkedDeque<MoanHolder<*>>()
  private val typedMap = ConcurrentHashMap<KClassifier, ConcurrentLinkedQueue<MoanHolder<*>>>()
  private val namedMap = ConcurrentHashMap<String, MoanHolder<*>>()
  private val closeListeners = ConcurrentLinkedDeque<() -> Unit>()
  private val uid = UIDS.getAndUpdate(BigInteger::inc)

  val path: String = generateSequence(this) { it.parent }.map { it.name }.reduce { a, b -> "$b/$a" }

  init {
    val name = this.path
    val queue = this.queue
    val typedMap = this.typedMap
    val namedMap = this.namedMap
    val closeListeners = this.closeListeners
    val closeTask = { uid: BigInteger ->
      closer(Runnable {
        name.asLogger.log(INFO, "Cleaning")
        close(uid, name, queue, typedMap, namedMap, closeListeners)
      })
    }
    if (parent != null) {
      val uid = this.uid
      val listener = { closeTask(uid).run() }
      val parent = this.parent
      parent.addCloseListener(listener)
      addCloseListener { parent.removeCloseListener(listener) }
    }
    CLEANABLES.computeIfAbsent(uid) { uid -> CLEANER.register(this, closeTask(uid)) }
  }

  fun addCloseListener(listener: () -> Unit) {
    closeListeners.addFirst(listener)
  }

  fun removeCloseListener(listener: () -> Unit) {
    closeListeners.removeIf { it === listener }
  }

  override fun by(type: KType, name: String?, optional: Boolean): MoanResult<Any?> {
    return when (type.classifier) {
      Seq::class -> {
        val list = byType(type).toList()
        if (list.isEmpty()) {
          when {
            optional -> MoanResult(null, true)
            type.isMarkedNullable -> MoanResult(null)
            else -> MoanResult(Seq(emptySequence<Any?>()))
          }
        } else {
          MoanResult(Seq(list.asSequence().map { it.moan }))
        }
      }
      Ref::class -> by(type.arguments.first().type!!, name, optional)
      Context::class -> {
        if (name == null) {
          MoanResult(this, false)
        } else {
          MoanResult(generateSequence(this) { it.parent }.find { it.name == name } ?: this, false)
        }
      }
      else -> {
        if (name == null) {
          by0(type, name, optional)
        } else {
          try {
            MoanResult(byName(name, type).moan)
          } catch (e: NoSuchElementException) {
            by0(type, name, optional)
          }
        }
      }
    }
  }

  private fun by0(type: KType, name: String?, optional: Boolean): MoanResult<Any?> {
    val list = byType(type).toList()
    return when (list.size) {
      0 -> when {
        optional -> MoanResult(null, true)
        type.isMarkedNullable -> MoanResult(null)
        else -> throw NoSuchElementException("No moan of type $type for $name")
      }
      1 -> MoanResult(list[0].moan)
      else -> throw MultipleBindingException("Multiple moans of type $type for $name: ${list.map { it.name }}")
    }
  }

  private fun byType(type: KType): Sequence<MoanHolder<*>> {
    val c = when (val c = type.classifier) {
      is KClassifier -> typedMap[c]?.let(Collection<MoanHolder<*>>::asSequence) ?: emptySequence()
      else -> queue.asSequence().filter { it.type.isSubtypeOf(type) }
    }
    return when (parent) {
      null -> c
      else -> c + parent.byType(type)
    }
  }

  private fun byName(name: String, type: KType): MoanHolder<*> {
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

  fun <T, H: MoanHolder<T>> register(holder: H, scope: Scope? = null) {
    namedMap.compute(holder.name) { n, old ->
      if (old == null) {
        if (scope != null && holder is ScopedMoanHolder<*>) {
          scope.add(holder, this)
        }
        queue += holder
        val type = holder.type
        when (val c = type.classifier) {
          is KClassifier -> typedMap.computeIfAbsent(c) { ConcurrentLinkedQueue<MoanHolder<*>>() } += holder
        }
        path.asLogger.log(INFO, "Registered $holder")
        holder
      } else {
        throw DuplicatedMoanException(n)
      }
    }
  }

  private fun unregister0(old: MoanHolder<*>): MoanHolder<*>? {
    queue.removeIf { it === old }
    val classifier = old.type.classifier
    if (classifier != null) {
      typedMap.computeIfPresent(classifier) { _, oldList ->
        oldList.removeIf { it === old }
        if (oldList.isEmpty()) null else oldList
      }
    }
    return null
  }

  fun unregister(name: String) {
    namedMap.computeIfPresent(name) { _, old -> unregister0(old) }
  }

  fun unregister(holder: MoanHolder<*>) {
    namedMap.computeIfPresent(holder.name) { _, old -> if (old === holder) unregister0(old) else old }
  }

  fun init(moduleFactory: (Context) -> Module, dependencyMapper: DependencyMapper = { sequenceOf(it) }): Context {
    try {
      val module = moduleFactory(this)
      module.initialize(dependencyMapper)
      return this
    } catch (e: Throwable) {
      try {
        close()
      } catch (x: Throwable) {
        e.addSuppressed(x)
      }
      throw e
    }
  }

  override fun close() = close(uid, name, queue, typedMap, namedMap, closeListeners)

  override fun toString(): String = path

  companion object {

    private val CLEANER = Cleaner.create()
    private val UIDS = AtomicReference(BigInteger.ZERO)
    private val CLEANABLES = ConcurrentHashMap<BigInteger, Cleaner.Cleanable>(128, 0.5f)

    init {
      Scope.cleanOnShutdown("ContextCleaner", CLEANABLES)
    }

    operator fun invoke(
      name: String,
      parent: Context? = null,
      closer: Closer = { it },
      properties: Properties = System.getProperties()
    ) = Context(name, parent, closer, properties)

    fun <T, H: MoanHolder<T>> H.withInitHook(hook: (T) -> Unit): H {
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
      val logger = name.asLogger
      while (it.hasNext()) {
        val e = it.next()
        try {
          logger.log(INFO, "Closing moan ${e.name}")
          e.close()
          logger.log(INFO, "Closed moan ${e.name}")
        } catch (x: Throwable) {
          exception.addSuppressed(x)
        } finally {
          namedMap.remove(e.name)
          val classifier = e.type.classifier
          if (classifier != null) {
            typedMap.computeIfPresent(classifier) { _, old ->
              old.removeIf { it === e }
              if (old.isEmpty()) null else old
            }
          }
          it.remove()
        }
      }
      try {
        check(queue.isEmpty()) { "queue is not empty" }
        check(typedMap.isEmpty()) { "typedMap is not empty" }
        check(namedMap.isEmpty()) { "namedMap is not empty" }
      } catch (e: Throwable) {
        exception.addSuppressed(e)
      }
      if (exception.suppressed.isNotEmpty()) {
        throw exception
      }
    }

    internal fun Context.args(callable: KCallable<*>): Map<KParameter, Any?> {
      val parameters = callable.parameters
      val args = mutableMapOf<KParameter, Any?>()
      for (p in parameters) {
        val propAnnotation = p.findAnnotation<Prop>()
        if (propAnnotation == null) {
          when (p.kind) {
            KParameter.Kind.INSTANCE -> args[p] = this
            KParameter.Kind.EXTENSION_RECEIVER -> throw IllegalStateException()
            KParameter.Kind.VALUE -> {
              val result = by(p.type, p.name, p.isOptional)
              if (!result.empty) {
                args[p] = result.value
              }
            }
          }
        } else {
          val propName = propAnnotation.value.ifEmpty { p.name }
          val propValue = properties.getProperty(propName)
          if (propValue == null) {
            if (!p.isOptional) {
              if (p.type.isMarkedNullable) {
                args[p] = null
              } else {
                throw IllegalArgumentException("Parameter ${p.name} of $callable has no default value")
              }
            }
          } else {
            val converted = convert(propValue, p.type)
            args[p] = converted
          }
        }
      }
      return args
    }
  }

  private fun convert(arg: String, to: KType): Any {
    val klass = to.classifier as KClass<*>
    return when (klass) {
      Int::class -> arg.toInt()
      Long::class -> arg.toLong()
      Boolean::class -> arg.toBoolean()
      String::class -> arg
      UInt::class -> arg.toUInt()
      ULong::class -> arg.toULong()
      List::class -> {
        val values = arg.split(',')
        val t = to.arguments[0].type!!
        values.map { convert(it.trim(), t) }
      }
      Set::class -> {
        val values = arg.split(',')
        val t = to.arguments[0].type!!
        values.map { convert(it.trim(), t) }.toSet()
      }
      SortedSet::class -> {
        val values = arg.split(',')
        val t = to.arguments[0].type!!
        TreeSet(values.map { convert(it.trim(), t) })
      }
      else -> throw IllegalArgumentException("Unable to convert $arg to $to")
    }
  }
}