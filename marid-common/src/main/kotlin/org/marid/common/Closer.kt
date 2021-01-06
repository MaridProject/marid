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
package org.marid.common

import java.nio.file.*
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class Closer {

  private val closeables = ConcurrentLinkedDeque<AutoCloseable>()

  fun use(closeable: AutoCloseable) {
    closeables += closeable
  }

  fun use(file: Path) {
    closeables += AutoCloseable {
      fun delete(f: Path) {
        try {
          Files.deleteIfExists(f)
        } catch (e: DirectoryNotEmptyException) {
          try {
            Files.newDirectoryStream(f).use {
              for (ff in it) {
                delete(ff)
              }
            }
            Files.deleteIfExists(f)
          } catch (x: NoSuchFileException) {
            // it's OK
          } catch (x: NotDirectoryException) {
            // it's OK
          }
        }
      }
      delete(file)
    }
  }

  fun use(executor: ExecutorService, duration: Duration = Duration.ofMinutes(1L)) {
    closeables += AutoCloseable {
      executor.shutdown()
      if (!executor.awaitTermination(duration.toNanos(), TimeUnit.NANOSECONDS)) {
        executor.shutdownNow()
        if (!executor.awaitTermination(duration.toNanos(), TimeUnit.NANOSECONDS)) {
          throw TimeoutException("Unable to close $executor")
        }
      }
    }
  }

  fun use(thread: Thread, interrupt: Boolean = false, duration: Duration = Duration.ofMinutes(1L)) {
    closeables += AutoCloseable {
      if (thread.isAlive) {
        if (interrupt) {
          thread.interrupt()
        }
        thread.join(duration.toMillis(), duration.toNanosPart() / 1000)
        if (thread.isAlive) {
          thread.interrupt()
          thread.join(duration.toMillis(), duration.toNanosPart() / 1000)
          if (thread.isAlive) {
            throw TimeoutException("Unable to close $thread")
          }
        }
      }
    }
  }

  private operator fun invoke() {
    var e: Throwable? = null
    val it = closeables.descendingIterator()
    while (it.hasNext()) {
      val c = it.next()
      try {
        c.close()
      } catch (x: Throwable) {
        when (e) {
          null -> e = x
          else -> e.addSuppressed(x)
        }
      } finally {
        it.remove()
      }
    }
    if (e != null) {
      throw e
    }
  }

  companion object {
    inline operator fun <T> invoke(f: Closer.() -> T): T {
      val c = Closer()
      val r = try {
        f(c)
      } catch (e: Throwable) {
        try {
          c()
        } catch (x: Throwable) {
          e.addSuppressed(x)
        }
        throw e
      }
      c()
      return r
    }
  }
}