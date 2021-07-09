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

import org.eclipse.jdt.core.compiler.CompilationProgress
import org.eclipse.jdt.internal.compiler.CompilationResult
import org.eclipse.jdt.internal.compiler.Compiler
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies.exitOnFirstError
import org.eclipse.jdt.internal.compiler.ICompilerRequestor
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration
import org.eclipse.jdt.internal.compiler.ast.SingleMemberAnnotation
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.eclipse.jdt.internal.compiler.lookup.Binding
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory
import org.marid.common.Closer
import org.marid.types.Layers
import org.marid.types.VarCode
import org.marid.types.VarName
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap

class TypeResolver(classpath: List<File>, progress: CompilationProgress?): AutoCloseable {

  private val results = ConcurrentLinkedDeque<CompilationResult>()
  private val units = ConcurrentLinkedDeque<CompilationUnitDeclaration>()
  private val requestor = ICompilerRequestor(results::offer)
  private val out = StringWriter()
  private val options = compilerOptions()
  private val fs = ResolverFileSystem(classpath)
  private val problems = DefaultProblemFactory()
  private val writer = PrintWriter(out)
  private val compiler = object: Compiler(fs, exitOnFirstError(), options, requestor, problems, writer, progress) {
    override fun process(unit: CompilationUnitDeclaration, i: Int) {
      super.process(unit, i)
      units += unit
    }
  }

  fun resolve(code: Map<VarName, VarCode>): Map<VarName, Binding> {
    val layers = toLayers(code, out)
    if (layers.isEmpty()) {
      return emptyMap()
    }
    cleanup()
    compiler.unitsToProcess = arrayOfNulls(layers.size)
    compiler.parseThreshold = layers.size
    val types = TreeMap<VarName, Binding>()
    layers.forEachIndexed { i, layer ->
      val w = StringWriter()
      val className = "C$i"
      w.appendLine("package infer;")
      w.appendLine("public class $className {")
      w.appendLine("  public void im() {")
      types.forEach { (v, b) -> w.appendLine("    @Var(\"${v.escaped}\") var ${v.jvmName} = ($b) null;") }
      layer.forEach { (v, c) -> w.appendLine("    @Var(\"${v.escaped}\") var ${v.jvmName} = ${c.resolved};") }
      w.appendLine("  }")
      w.appendLine("}")
      val cu = CompilationUnit(w.toString().toCharArray(), "infer/$className.java", "UTF-8")
      compiler.compile(arrayOf(cu))
      val unit = units.peekLast() ?: return@forEachIndexed
      val type = unit.types.find { String(it.name) == className } ?: return@forEachIndexed
      val method = type.methods.find { it is MethodDeclaration && String(it.selector) == "im" } ?: return@forEachIndexed
      for (st in method.statements) {
        if (st is LocalDeclaration) {
          val annotations = st.annotations ?: emptyArray()
          val ann = annotations.asSequence()
            .filterIsInstance<SingleMemberAnnotation>()
            .find { it.type.typeName.joinToString(".", transform = ::String) == "Var" } ?: continue
          val name = ann.memberValue.constant.stringValue()
          val binding = st.binding
          types[VarName(name)] = binding
          when (val t = binding.type) {
            is ParameterizedTypeBinding -> {
              println(t.methods())
            }
          }
        }
      }
    }
    return types
  }

  private fun cleanup() {
    compiler.reset()
    out.buffer.setLength(0)
    out.buffer.trimToSize()
    results.clear()
  }

  override fun close() {
    Closer {
      register { fs.cleanup() }
      register { cleanup() }
    }
  }
}

private fun compilerOptions(): CompilerOptions {
  val opts = CompilerOptions()
  val level = ClassFileConstants.JDK16
  opts.complianceLevel = level
  opts.sourceLevel = level
  opts.generateClassFiles = false
  opts.preserveAllLocalVariables = true
  opts.produceMethodParameters = true
  opts.produceReferenceInfo = true
  opts.targetJDK = level
  opts.ignoreMethodBodies = false
  opts.originalComplianceLevel = level
  opts.originalSourceLevel = level
  opts.processAnnotations = true
  opts.storeAnnotations = true
  return opts
}

internal fun toLayers(map: Map<VarName, VarCode>, writer: StringWriter): Layers {
  val mmap = ConcurrentSkipListMap(map)
  val result = ConcurrentLinkedQueue(
    listOf(ArrayList(
      map.entries.flatMap { (k, v) ->
        if (v.isRoot) {
          mmap.remove(k)
          listOf(k to v)
        } else emptyList()
      }
    ))
  )
  result.removeIf { it.isEmpty() }
  while (mmap.isNotEmpty()) {
    val layer = ArrayList(mmap.entries.flatMap { (k, v) ->
      if (v.varNames.all { n -> result.any { r -> r.any { e -> e.first == n } } }) {
        mmap.remove(k)
        listOf(k to v)
      } else emptyList()
    })
    if (layer.isEmpty()) {
      writer.appendLine("Circular dependencies: $mmap")
      break
    } else {
      result += layer
    }
  }
  return result
}
