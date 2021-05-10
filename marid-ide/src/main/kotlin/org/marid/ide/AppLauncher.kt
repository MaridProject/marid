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
package org.marid.ide

import org.marid.ide.logging.IdeLogManager
import java.net.URL
import java.net.URLClassLoader
import java.util.regex.Pattern
import kotlin.io.path.Path

object AppLauncher {
  @JvmStatic fun main(args: Array<String>) {
    // initialize logging
    System.setProperty("java.util.logging.manager", IdeLogManager::class.java.name)
    val classpath = System.getProperty("java.class.path")
    val controlsRegex = Pattern.compile(".+javafx-controls-(.+)[.]jar$")
    var ver = ""
    val urls = classpath.split(':')
      .map { Path(it).toUri().toURL() }
      .onEach { url ->
        val matcher = controlsRegex.matcher(url.path)
        if (matcher.matches()) {
          ver = matcher.group(1)
        }
      }
    val remoteUrls = listOf("base", "graphics", "controls", "media", "swing")
      .map { URL("https://repo1.maven.org/maven2/org/openjfx/javafx-$it/$ver/javafx-$it-$ver-${os()}.jar") }
    val cl = URLClassLoader("marid", (urls + remoteUrls).toTypedArray(), ClassLoader.getPlatformClassLoader())
    val applicationClass = cl.loadClass("javafx.application.Application")
    val appClass = cl.loadClass("org.marid.ide.App")
    val launchMethod = applicationClass.getMethod("launch", Class::class.java, Array<String>::class.java)
    launchMethod.invoke(null, appClass, args)
  }

  private fun os(): String {
    val text = System.getProperty("os.name").lowercase()
    return when {
      text.contains("win") -> "win"
      text.contains("mac") -> "mac"
      else -> "linux"
    }
  }
}