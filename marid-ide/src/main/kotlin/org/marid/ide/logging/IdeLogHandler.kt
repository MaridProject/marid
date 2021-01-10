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
package org.marid.ide.logging

import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.text.MessageFormat
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofPattern
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.LogRecord

object IdeLogHandler : Handler() {

  private val outputDir = Files.createDirectories(Path.of(System.getProperty("user.home"), ".marid", "logs"))
  private val outputFile = outputDir.resolve(now().format(ofPattern("yyyy-MM-dd-HH-mm-ss-SSS'.log'")))
  private val channel = FileOutputStream(outputFile.toFile())
  private val dtFormat = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

  init {
    Files.newDirectoryStream(outputDir, "*.log").use { logs ->
      val map = TreeMap<FileTime, MutableList<Path>>()
      logs.forEach { log -> map.computeIfAbsent(Files.getLastModifiedTime(log)) { mutableListOf() }.add(log) }
      val toTime = FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7L))
      for ((time, files) in map.headMap(toTime, true)) {
        for (file in files) {
          try {
            Files.deleteIfExists(file)
          } catch (e: Throwable) {
            IOException("Unable to delete file $file ($time)", e).printStackTrace()
          }
        }
      }
    }
  }

  override fun publish(record: LogRecord) {
    if (record.message == null) {
      return
    }
    if (!isLoggable(record)) {
      return
    }
    val buf = ByteArrayOutputStream(128)
    val fmt = PrintWriter(OutputStreamWriter(buf, StandardCharsets.UTF_8))
    fmt.append(LocalDateTime.ofInstant(record.instant, ZoneId.systemDefault()).format(dtFormat))
    fmt.append(' ').append(record.level.name).append(' ')
    fmt.append(record.sourceClassName?.let { it + "." + record.sourceMethodName } ?: record.loggerName).append(' ')
    if (record.parameters.isNullOrEmpty()) {
      fmt.append(record.message)
    } else {
      try {
        fmt.append(MessageFormat.format(record.message, record.parameters))
      } catch (_: Throwable) {
        fmt.append(record.message)
      }
    }
    fmt.println()
    record.thrown?.printStackTrace(fmt)
    try {
      buf.writeTo(channel)
    } catch (_: Throwable) {
      // do nothing
    }
  }

  override fun flush() {
    channel.flush()
  }

  override fun close() {
    channel.close()
  }
}