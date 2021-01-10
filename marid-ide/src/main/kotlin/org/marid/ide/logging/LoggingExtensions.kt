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
@file:Suppress("NOTHING_TO_INLINE")

package org.marid.ide.logging

import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

inline val Any.log: Logger get() = Logger.getLogger(this.javaClass.name)

inline val LOG_INFO: Level get() = Level.INFO
inline val LOG_WARNING: Level get() = Level.WARNING
inline val LOG_ERROR: Level get() = Level.SEVERE
inline val LOG_CONFIG: Level get() = Level.CONFIG
inline val LOG_TRACE: Level get() = Level.FINE

inline operator fun Logger.invoke(level: Level, message: String) {
  val record = LogRecord(level, message)
  record.loggerName = name
  log(record)
}

inline operator fun Logger.invoke(level: Level, message: String, thrown: Throwable) {
  val record = LogRecord(level, message)
  record.loggerName = name
  record.thrown = thrown
  log(record)
}

inline operator fun Logger.invoke(level: Level, message: String, thrown: Throwable, vararg args: Any?) {
  val record = LogRecord(level, message)
  record.loggerName = name
  record.thrown = thrown
  record.parameters = args
  log(record)
}

inline operator fun Logger.invoke(level: Level, message: String, vararg args: Any?) {
  val record = LogRecord(level, message)
  record.loggerName = name
  record.parameters = args
  log(record)
}