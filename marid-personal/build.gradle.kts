import org.jetbrains.kotlin.js.parser.sourcemaps.JsonObject
import org.jetbrains.kotlin.js.parser.sourcemaps.JsonString
import org.jetbrains.kotlin.js.parser.sourcemaps.parseJson
import java.util.regex.Pattern

plugins {
  application
  kotlin("jvm")
  id("com.github.node-gradle.node").version("3.1.0")
}

application {
  mainClass.set("org.marid.personal.Site")
}

configurations.all {
  resolutionStrategy.eachDependency {
    when (requested.group) {
      "org.jboss.xnio" -> useVersion("3.8.4.Final")
      "org.jboss.threads" -> useVersion("3.3.0.Final")
    }
  }
}

dependencies {
  implementation(project(":marid-moans"))

  implementation(group = "io.undertow", name = "undertow-core", version = "2.2.7.Final")
  implementation(group = "org.slf4j", name = "slf4j-jdk14", version = "1.7.30")
}