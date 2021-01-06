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
package org.marid.types

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.marid.common.Closer
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.tools.Diagnostic
import javax.tools.DiagnosticListener
import javax.tools.FileObject
import javax.tools.StandardLocation.*
import javax.tools.ToolProvider.getSystemJavaCompiler

class TypeResolver : AutoCloseable {

  private val fileSystem = Jimfs.newFileSystem(Configuration.unix())
  private val baseDir = fileSystem.getPath("/work")
  private val classOutputDir = baseDir.resolve("classes")
  private val srcDir = baseDir.resolve("src")
  private val genSrc = baseDir.resolve("genSrc")

  private val javaCompiler = getSystemJavaCompiler()
  private val diagnosticQueue = ConcurrentLinkedQueue<Diagnostic<out FileObject>>()
  private val diagnostics = DiagnosticListener<FileObject> { diagnosticQueue += it }

  private val fileManager = javaCompiler.getStandardFileManager(diagnostics, Locale.US, UTF_8)

  init {
    fileManager.setLocationFromPaths(CLASS_OUTPUT, listOf(classOutputDir))
    fileManager.setLocationFromPaths(SOURCE_PATH, listOf(srcDir))
    fileManager.setLocationFromPaths(SOURCE_OUTPUT, listOf(genSrc))
  }

  @Suppress("UnstableApiUsage")
  private fun cleanDir(dir: Path) {
    MoreFiles.deleteDirectoryContents(dir, RecursiveDeleteOption.ALLOW_INSECURE)
  }

  fun classPath(path: Collection<Path>) {
    fileManager.setLocationFromPaths(CLASS_PATH, path)
  }

  override fun close() {
    Closer {
      use(fileSystem)
    }
  }
}