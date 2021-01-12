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
@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package org.marid.moan

import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

@Suppress("NOTHING_TO_INLINE")
internal inline class LoggerWrapper(val logger: Logger) {
  inline fun log(level: Level, message: String, thrown: Throwable? = null) {
    val record = LogRecord(level, message)
    record.loggerName = logger.name
    record.sourceMethodName = null
    record.thrown = thrown
    logger.log(record)
  }
}

internal inline val String.asLogger get() = LoggerWrapper(Logger.getLogger(this))

typealias DependencyMapper = (Module) -> Sequence<Module>