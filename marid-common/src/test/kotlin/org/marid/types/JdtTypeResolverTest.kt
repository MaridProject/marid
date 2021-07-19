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