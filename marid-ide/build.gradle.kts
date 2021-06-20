plugins {
  kotlin("jvm")
}

val openjfxVersion = "16"

dependencies {
  compileOnly(project(":marid-common"))
  compileOnly(project(":marid-moans"))

  compileOnly(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json-jvm", version = "1.2.1")
  compileOnly(group = "org.slf4j", name = "slf4j-jdk14", version = "1.7.30")

  compileOnly(group = "org.openjfx", name = "javafx-controls", version = openjfxVersion)
  compileOnly(group = "org.openjfx", name = "javafx-media", version = openjfxVersion)
  compileOnly(group = "org.openjfx", name = "javafx-swing", version = openjfxVersion)

  for (os in listOf("linux", "mac", "win")) {
    compileOnly(group = "org.openjfx", name = "javafx-controls", version = openjfxVersion, classifier = os)
    compileOnly(group = "org.openjfx", name = "javafx-media", version = openjfxVersion, classifier = os)
    compileOnly(group = "org.openjfx", name = "javafx-base", version = openjfxVersion, classifier = os)
    compileOnly(group = "org.openjfx", name = "javafx-swing", version = openjfxVersion, classifier = os)
    compileOnly(group = "org.openjfx", name = "javafx-graphics", version = openjfxVersion, classifier = os)
  }

  testImplementation(project(":marid-test"))
}

tasks.getByName("processResources", ProcessResources::class) {
  doLast {
    val depsDir = destinationDir.resolve("deps").also { it.mkdirs() }
    destinationDir.resolve("deps.list").printWriter().use { w ->
      project.configurations.compileOnly.get().also { c ->
        c.incoming.artifacts.artifactFiles.forEach {
          val fileName = it.name
          w.println(fileName)
          val destFile = depsDir.resolve(fileName)
          it.copyTo(destFile, true, 65536)
        }
      }
    }
  }
}

tasks.getByName("jar", org.gradle.jvm.tasks.Jar::class) {
  manifest {
    attributes("Main-Class" to "org.marid.ide.AppLauncher")
  }
}