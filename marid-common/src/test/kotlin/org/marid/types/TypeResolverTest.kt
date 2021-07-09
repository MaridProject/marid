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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.of
import org.junit.jupiter.params.provider.MethodSource
import org.marid.resolver.TypeResolver
import org.marid.resolver.toLayers
import java.io.StringWriter
import java.util.*
import java.util.stream.Stream

class TypeResolverTest {

  private val resolver = TypeResolver(listOf(), null)

  @ParameterizedTest
  @MethodSource("toLayersData")
  fun toLayersSimple(map: Map<String, String>, expected: List<List<Pair<String, String>>>, err: List<String>) {
    val code = TreeMap<VarName, VarCode>().also { map.forEach { k, v -> it[VarName(k)] = VarCode(v) } }
    val writer = StringWriter()
    val result = toLayers(code, writer)
    val actual = result.map { l -> l.map { (k, v) -> k.toString() to v.toString() } }
    val actualErrors = writer.buffer.lineSequence().filterNot { it.isBlank() }.toList()
    assertEquals(expected, actual)
    assertEquals(err, actualErrors)
  }

  @ParameterizedTest
  @MethodSource("resolveData")
  fun resolve(map: Map<String, String>, expected: Map<String, String>, err: List<String>) {
    val input = TreeMap<VarName, VarCode>().also { m -> map.forEach { k, v -> m[VarName(k)] = VarCode(v) } }
    val res = resolver.resolve(input)
    println(res)
  }

  companion object {
    @JvmStatic fun toLayersData() = Stream.of(
      of(
        mapOf("a" to "1"),
        listOf(listOf("a" to "1")),
        listOf<String>()
      ),
      of(
        mapOf("a" to "@{b}", "b" to "1"),
        listOf(listOf("b" to "1"), listOf("a" to "@{b}")),
        listOf<String>()
      ),
      of(
        mapOf("a" to "@{b}", "b" to "@{c}", "c" to "2"),
        listOf(listOf("c" to "2"), listOf("b" to "@{c}"), listOf("a" to "@{b}")),
        listOf<String>()
      ),
      of(
        mapOf("a" to "@{b}", "b" to "@{c}", "c" to "2", "d" to "@{c}"),
        listOf(listOf("c" to "2"), listOf("b" to "@{c}", "d" to "@{c}"), listOf("a" to "@{b}")),
        listOf<String>()
      ),
      of(
        mapOf("a" to "@{b}", "b" to "@{c}", "d" to "@{c}"),
        listOf<List<Pair<String, String>>>(),
        listOf("Circular dependencies: {a=@{b}, b=@{c}, d=@{c}}")
      ),
      of(
        mapOf("a" to "@{b}", "b" to "@{c}", "d" to "@{c}", "e" to "2"),
        listOf(listOf("e" to "2")),
        listOf("Circular dependencies: {a=@{b}, b=@{c}, d=@{c}}")
      )
    )

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