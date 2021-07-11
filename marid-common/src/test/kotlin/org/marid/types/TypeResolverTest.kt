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

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.of
import org.junit.jupiter.params.provider.MethodSource
import org.marid.resolver.Task
import org.marid.resolver.TypeResolver
import java.util.stream.Stream

class TypeResolverTest {

  private val resolver = TypeResolver(listOf(), null)

  @ParameterizedTest
  @MethodSource("resolveData")
  fun resolve(map: Map<String, String>, expected: Map<String, String>, err: List<String>) {
    val task = Task().also { map.forEach { (k, v) -> it.add(k, v) } }
    val res = resolver.resolve(task)
    println(res)
  }

  companion object {
    @JvmStatic fun resolveData() = Stream.of(
      of(
        mapOf("a" to "1"),
        mapOf("a" to "int"),
        listOf<String>()
      ),
      of(
        mapOf("a" to "java.util.List.of(1, 2.0)"),
        mapOf("a" to "java.util.List<java.lang.Number&java.lang.Comparable<? extends java.lang.Number&java.lang.Comparable<?>&java.lang.constant.Constable&java.lang.constant.ConstantDesc>&java.lang.constant.Constable&java.lang.constant.ConstantDesc>"),
        listOf<String>()
      )
    )
  }
}