package org.marid.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.of
import org.junit.jupiter.params.provider.MethodSource
import java.io.StringWriter
import java.util.*
import java.util.stream.Stream

class TypeResolverTest {

  @ParameterizedTest
  @MethodSource("toLayersData")
  fun toLayersSimple(map: Map<String, String>, expected: List<List<Pair<String, String>>>, err: List<String>) {
    val code = TreeMap<VarName, VarCode>().also { map.forEach { k, v -> it[VarName(k)] = VarCode(v) } }
    val writer = StringWriter()
    val result = TypeResolver().toLayers(code, writer)
    val actual = result.map { l -> l.map { (k, v) -> k.toString() to v.toString() } }
    val actualErrors = writer.buffer.lineSequence().filterNot { it.isBlank() }.toList()
    assertEquals(expected, actual)
    assertEquals(err, actualErrors)
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
  }
}