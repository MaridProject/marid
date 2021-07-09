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
package org.marid.resolver

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import org.eclipse.jdt.internal.compiler.batch.ClasspathJrt
import org.eclipse.jdt.internal.compiler.batch.FileSystem
import java.io.File
import java.nio.file.Files

class ResolverFileSystem(classpath: List<File>): FileSystem(makeClassPath(classpath), null, true) {

  override fun cleanup() {
    super.cleanup()
    for (cp in classpaths) {
      val file = File(cp.path)
      if (file.name.startsWith("ecjcp")) {
        @Suppress("UnstableApiUsage")
        MoreFiles.deleteRecursively(file.toPath(), RecursiveDeleteOption.ALLOW_INSECURE)
        return
      }
    }
  }
}

private fun makeClassPath(classpath: List<File>): Array<FileSystem.Classpath> {
  val list = ArrayList<FileSystem.Classpath>(classpath.size + 2)
  val javaHome = System.getProperty("java.home")
  list += ClasspathJrt(File(javaHome), true, null, null)
  val tempDirectory = Files.createTempDirectory("ecjcp")
  val inferDir = tempDirectory.resolve("infer").also(Files::createDirectory)
  for (className in listOf("Var")) {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("infer/$className.class")!!
    stream.use { Files.copy(it, inferDir.resolve("$className.class")) }
  }
  list += FileSystem.getClasspath(tempDirectory.toAbsolutePath().toString(), "UTF-8", null)
  classpath.forEach { list += FileSystem.getClasspath(it.absolutePath, "UTF-8", null) }
  return list.toTypedArray()
}