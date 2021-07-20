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

import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.*
import org.junit.jupiter.api.Test

class JdtTypeResolverTest {
  @Test
  fun test() {
    val parser = ASTParser.newParser(AST.JLS_Latest)
    parser.setResolveBindings(true)
    parser.setEnvironment(null, null, null, true)
    parser.setCompilerOptions(
      mapOf(
        JavaCore.COMPILER_RELEASE to "enabled",
        JavaCore.COMPILER_SOURCE to "16",
        JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM to "16"
      )
    )
    parser.setBindingsRecovery(true)
    parser.setStatementsRecovery(true)
    parser.setKind(ASTParser.K_COMPILATION_UNIT)
    parser.setUnitName("X.java")
    parser.setSource(
      """
      public class X {
        public void x() {
          var z = java.util.List.of(1, 1.0);
        }
      }
    """.trimIndent().toCharArray()
    )
    val cu = parser.createAST(null) as CompilationUnit
    println(cu.ast.hasResolvedBindings())
    cu.accept(object: ASTVisitor() {
      override fun visit(node: VariableDeclarationFragment): Boolean {
        val typeBinding = node.resolveBinding().variableDeclaration.type
        println(typeBinding)
        return super.visit(node)
      }
    })
    println(cu)
  }
}