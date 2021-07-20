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

import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.*
import org.marid.resolver.Task.Companion.append
import java.io.File
import java.io.StringWriter

class TypeResolver(classpath: List<File>): AutoCloseable {

  private val classpath = classpath.takeIf(List<*>::isNotEmpty)?.map(File::toString)?.toTypedArray()

  fun resolve(task: Task): ResolverResult {
    val result = ResolverResult()
    val tasks = task.toLayers()
    if (tasks.isEmpty()) {
      return result
    }
    val parser = ASTParser.newParser(AST.JLS_Latest)
    parser.setResolveBindings(true)
    parser.setEnvironment(classpath, null, null, true)
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
    val w = StringWriter()
    val className = "C"
    parser.setUnitName("$className.java")
    w.appendLine("public class $className {")
    w.appendLine("  public void im() {")
    tasks.forEach { layer ->
      w.append("    ", layer)
    }
    w.appendLine("  }")
    w.appendLine("}")
    val code = w.toString()
    parser.setSource(code.toCharArray())
    val cu = parser.createAST(null) as CompilationUnit
    cu.accept(object: ASTVisitor() {
      override fun visit(node: VariableDeclarationFragment): Boolean {
        val typeBinding = node.resolveBinding().variableDeclaration.type
        result.add(Task.fromJvmName(node.name.identifier), typeBinding)
        return super.visit(node)
      }
    })
    return result
  }

  override fun close() {
  }
}