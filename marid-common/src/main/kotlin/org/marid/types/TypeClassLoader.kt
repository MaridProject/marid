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

import org.marid.common.Cleaners.CLEANER
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import java.util.stream.Stream
import kotlin.reflect.KClass

class TypeClassLoader(private val delegate: ClassLoader) {

  init {
    if (delegate is URLClassLoader) {
      val d = delegate
      CLEANER.register(this) { d.close() }
    }
  }

  fun loadKlass(className: String): KClass<*> = loadClass(className).kotlin
  fun loadClass(className: String): Class<*> = delegate.loadClass(className)
  fun resource(name: String): URL? = delegate.getResource(name)
  fun resources(name: String): Stream<URL> = delegate.resources(name)
  fun stream(name: String): InputStream? = delegate.getResourceAsStream(name)
  fun reader(name: String): BufferedReader? = stream(name)?.let { BufferedReader(InputStreamReader(it, UTF_8)) }
  fun scanner(name: String): Scanner? = stream(name)?.let { Scanner(it, UTF_8) }
}