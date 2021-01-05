package org.marid.moan

import org.marid.moan.Context.Companion.ScopedMoanHolder
import java.lang.ref.Cleaner
import java.math.BigInteger
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.concurrent.thread

class Scope(val name: String) : AutoCloseable {

  private val moans = ConcurrentLinkedDeque<ScopedMoanHolder<*>>()
  private val uid = nextUid()

  internal fun add(holder: ScopedMoanHolder<*>) {
    CLEANABLES.computeIfAbsent(uid) { uid ->
      val moans = this.moans
      val contextName = name
      CLEANER.register(this) {
        try {
          close(contextName, moans)
        } catch (e: Throwable) {
          val logger = Logger.getLogger("Scope")
          val record = LogRecord(Level.WARNING, "Unable to close scope")
          record.thrown = e
          record.loggerName = "Scope"
          record.sourceClassName = null
          logger.log(record)
        } finally {
          CLEANABLES.remove(uid)
        }
      }
    }
    moans += holder
  }

  override fun close() {
    try {
      close(name, moans)
    } finally {
      CLEANABLES.remove(uid)
    }
  }

  override fun toString(): String = "Context($name)"

  companion object {

    private val UIDS = AtomicReference(BigInteger.ZERO)
    private val CLEANER = Cleaner.create()
    private val CLEANABLES = ConcurrentSkipListMap<BigInteger, Cleaner.Cleanable>()

    init {
      Runtime.getRuntime().addShutdownHook(thread(name = "ScopeCleaner") {
        CLEANABLES.descendingMap().entries.removeIf {
          val c = it.value
          try {
            c.clean()
          } catch (e: Throwable) {
            // do nothing since the cleanable is responsible to do it well
          }
          true
        }
      })
    }

    private fun nextUid() = UIDS.getAndUpdate { it.inc() }

    private fun close(name: String, moans: ConcurrentLinkedDeque<ScopedMoanHolder<*>>) {
      val ex = ScopeDestructionException(name)
      val it = moans.descendingIterator()
      while (it.hasNext()) {
        val moan = it.next()
        try {
          moan.close()
        } catch (x: Throwable) {
          ex.addSuppressed(x)
        } finally {
          it.remove()
        }
      }
      if (ex.suppressed.isNotEmpty()) {
        throw ex
      }
    }
  }
}