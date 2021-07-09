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

import com.sun.source.util.Trees
import java.util.concurrent.ConcurrentLinkedQueue
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

typealias Pairs = List<Pair<VarName, VarCode>>
typealias Layers = ConcurrentLinkedQueue<out Pairs>

sealed interface TypeResult {
  val allTypes: Map<VarName, TypeMirror>
  val errors: List<String>
}

@JvmRecord
data class NormalTypeResult(
  override val allTypes: Map<VarName, TypeMirror>,
  val types: Types,
  val elements: Elements,
  val trees: Trees,
  val classLoader: TypeClassLoader,
  override val errors: List<String>
): TypeResult

@JvmRecord
data class ErrorTypeResult(
  override val errors: List<String>
): TypeResult {
  override val allTypes: Map<VarName, TypeMirror> get() = emptyMap()
}