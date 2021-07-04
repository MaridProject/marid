package org.marid.types

import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

sealed interface TypeResult {
  val allTypes: Map<VarName, TypeMirror>
  val errors: List<String>
}

@JvmRecord
data class NormalTypeResult(
  override val allTypes: Map<VarName, TypeMirror>,
  val types: Types,
  override val errors: List<String>
): TypeResult

@JvmRecord
data class ErrorTypeResult(
  override val errors: List<String>
): TypeResult {
  override val allTypes: Map<VarName, TypeMirror> get() = emptyMap()
}