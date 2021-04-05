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

@JvmInline
value class MaridLogger(val logger: Logger) {

  inline fun record(level: Level, message: String): LogRecord {
    val record = LogRecord(level, message)
    record.loggerName = logger.name
    return record
  }

  inline fun log(level: Level, message: String) {
    logger.log(record(level, message))
  }

  inline fun log(level: Level, message: String, thrown: Throwable) {
    logger.log(record(level, message).also { it.thrown = thrown })
  }

  inline fun log(level: Level, message: String, thrown: Throwable, vararg args: Any?) {
    logger.log(record(level, message).also { it.thrown = thrown; it.parameters = args })
  }

  inline fun log(level: Level, message: String, vararg args: Any?) {
    logger.log(record(level, message).also { it.parameters = args })
  }

  inline fun info(message: String) {
    logger.log(record(Level.INFO, message))
  }

  inline fun info(message: String, vararg args: Any?) {
    logger.log(record(Level.INFO, message).also { it.parameters = args })
  }

  inline fun warn(message: String) {
    logger.log(record(Level.WARNING, message))
  }

  inline fun warn(message: String, thrown: Throwable) {
    logger.log(record(Level.WARNING, message).also { it.thrown = thrown })
  }

  inline fun warn(message: String, thrown: Throwable, vararg args: Any?) {
    logger.log(record(Level.WARNING, message).also { it.thrown = thrown; it.parameters = args })
  }

  inline fun warn(message: String, vararg args: Any?) {
    logger.log(record(Level.WARNING, message).also { it.parameters = args })
  }

  inline fun error(message: String) {
    logger.log(record(Level.SEVERE, message))
  }

  inline fun error(message: String, thrown: Throwable) {
    logger.log(record(Level.SEVERE, message).also { it.thrown = thrown })
  }

  inline fun error(message: String, thrown: Throwable, vararg args: Any?) {
    logger.log(record(Level.SEVERE, message).also { it.thrown = thrown; it.parameters = args })
  }

  inline fun error(message: String, vararg args: Any?) {
    logger.log(record(Level.SEVERE, message).also { it.parameters = args })
  }

  inline fun config(message: String) {
    logger.log(record(Level.CONFIG, message))
  }

  inline fun config(message: String, thrown: Throwable) {
    logger.log(record(Level.CONFIG, message).also { it.thrown = thrown })
  }

  inline fun config(message: String, thrown: Throwable, vararg args: Any?) {
    logger.log(record(Level.CONFIG, message).also { it.thrown = thrown; it.parameters = args })
  }

  inline fun config(message: String, vararg args: Any?) {
    logger.log(record(Level.CONFIG, message).also { it.parameters = args })
  }

  inline fun trace(message: String) {
    logger.log(record(Level.FINE, message))
  }

  inline fun trace(message: String, thrown: Throwable) {
    logger.log(record(Level.FINE, message).also { it.thrown = thrown })
  }

  inline fun trace(message: String, thrown: Throwable, vararg args: Any?) {
    logger.log(record(Level.FINE, message).also { it.thrown = thrown; it.parameters = args })
  }

  inline fun trace(message: String, vararg args: Any?) {
    logger.log(record(Level.FINE, message).also { it.parameters = args })
  }
}

inline val Any.logger: MaridLogger get() = MaridLogger(Logger.getLogger(this::javaClass.name))