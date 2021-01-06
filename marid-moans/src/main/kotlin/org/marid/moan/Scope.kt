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

  override fun toString(): String = "Scope($name)"

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
      moans.removeIf { moan ->
        try {
          moan.close()
        } catch (x: Throwable) {
          ex.addSuppressed(x)
        }
        true
      }
      if (ex.suppressed.isNotEmpty()) {
        throw ex
      }
    }
  }
}