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

import com.google.common.jimfs.Jimfs
import org.marid.common.Closer
import java.io.StringWriter
import java.lang.Thread.currentThread
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.*

typealias Completions = Iterable<Completion>
typealias AM = AnnotationMirror
typealias Pairs = List<Pair<VarName, VarCode>>
typealias Layers = ConcurrentLinkedQueue<out Pairs>

class TypeResolver {

  private val compiler = ToolProvider.getSystemJavaCompiler()

  fun resolve(classpath: List<Path>, code: Map<VarName, VarCode>): TypeResult {
    val writer = StringWriter()
    val fileCounter = AtomicInteger()

    // check layers
    val layers = toLayers(code, writer)
    if (layers.isEmpty()) {
      return ErrorTypeResult(listOf("Empty code"))
    }

    // diagnostics
    val diagnosticsQueue = ConcurrentLinkedQueue<Diagnostic<out JavaFileObject>>()
    val diagnostics = DiagnosticListener<JavaFileObject>(diagnosticsQueue::offer)

    val result = ConcurrentSkipListMap<VarName, TypeMirror>()
    Closer {
      val fs = use(Jimfs.newFileSystem())
      val fm = use(compiler.getStandardFileManager(diagnostics, Locale.US, StandardCharsets.UTF_8))
      fm.setLocationFromPaths(StandardLocation.CLASS_PATH, classpath)

      // prepare directories
      val rootDir = fs.rootDirectories.first()
      val sourceDir = rootDir.resolve("src").also(Files::createDirectory)
      val genDir = rootDir.resolve("gen").also(Files::createDirectory)
      val outDir = rootDir.resolve("out").also(Files::createDirectory)
      val annDir = rootDir.resolve("ann").also(Files::createDirectory)

      // register directories
      fm.setLocationFromPaths(StandardLocation.SOURCE_PATH, listOf(sourceDir))
      fm.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, listOf(genDir))
      fm.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, listOf(outDir))
      fm.setLocationFromPaths(StandardLocation.ANNOTATION_PROCESSOR_PATH, listOf(annDir))

      // copy Var annotation
      currentThread().contextClassLoader.getResource("Var.class")!!.openStream().use { s ->
        Files.copy(s, annDir.resolve("Var.class"))
      }

      // creating the compiler task
      val task = compiler.getTask(
        writer,
        fm,
        diagnostics,
        listOf("-parameters"),
        null, // a processor will be defined later
        listOf(writeCode(sourceDir, layers.peek(), fm, fileCounter))
      )

      // processor
      val processor = object: Processor {
        lateinit var env: ProcessingEnvironment
        lateinit var types: Types
        lateinit var elements: Elements
        lateinit var filer: Filer
        lateinit var messager: Messager

        override fun init(processingEnv: ProcessingEnvironment) {
          env = processingEnv
          types = processingEnv.typeUtils
          elements = processingEnv.elementUtils
          filer = processingEnv.filer
          messager = processingEnv.messager
        }

        override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
          val elements = roundEnv.getElementsAnnotatedWithAny(*annotations.toTypedArray())
          if (elements.isEmpty()) {
            return false
          }
          val layer = layers.poll()
          if (layer == null) {
            writer.appendLine("Empty layer: $elements")
            return false
          }
          val pairs = TreeMap<VarName, VarCode>().also { m -> layer.forEach { (k, v) -> m[k] = v } }
          for (e in elements) {
            if (e is VariableElement) {
              for (am in e.annotationMirrors) {
                if (annotations.contains(am.annotationType.asElement())) {
                  val name = VarName(am.elementValues.values.first().value.toString())
                  val type = e.asType()
                  result[name] = type
                  if (pairs.remove(name) == null) {
                    writer.appendLine("Absent name: $name")
                  }
                }
              }
            }
          }

          if (pairs.isNotEmpty()) {
            writer.appendLine("Non-empty pairs: $pairs")
            return false
          }
          val nextLayer = layers.peek()
          if (nextLayer != null) {

          }
          return false
        }

        override fun getSupportedAnnotationTypes(): Set<String> = setOf("Var")
        override fun getSupportedOptions(): Set<String> = setOf()
        override fun getCompletions(e: Element, a: AM, m: ExecutableElement, t: String): Completions = emptyList()
        override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.RELEASE_16
      }
      task.setProcessors(listOf(processor))

      // run task
      task.call()
      return NormalTypeResult(
        result,
        processor.types,
        writer.buffer.lineSequence().filterNot(String::isBlank).toList()
      )
    }
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

  private fun code(code: Pairs, fc: AtomicInteger): Pair<String, String> {
    val name = "F_${fc.getAndIncrement()}"
    val vc = AtomicInteger()
    val varNames = LinkedHashSet<String>()
    val varsText = code.joinToString("\n", "\n", "\n") { (k, v) ->
      val varName = "v_${vc.getAndIncrement()}".also(varNames::add)
      val escapedName = k.toString().replace("\"\"\"", "'''")
      "    @Var(\"\"\"$escapedName\"\"\") var $varName = ${v.resolved};"
    }
    val consumeVarsText = varNames.joinToString("\n", "\n", "\n") { v ->
      "    System.out.println($v);"
    }
    val classText = """
      package infer;
      public class $name {
        public void method() {
          $varsText
          $consumeVarsText
        }
      }
    """.trimIndent()
    return name to classText
  }

  private fun writeCode(dir: Path, code: Pairs, fm: JavaFileManager, fc: AtomicInteger): JavaFileObject {
    val (name, classText) = code(code, fc)
    val file = dir.resolve("infer").also(Files::createDirectories).resolve("$name.java")
    Files.writeString(file, classText, StandardCharsets.UTF_8)
    return fm.getJavaFileForInput(StandardLocation.SOURCE_PATH, "infer.$name", JavaFileObject.Kind.SOURCE)
  }
}