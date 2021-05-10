plugins {
  application
  kotlin("jvm")
}

val openjfxVersion = "16"

dependencies {
  implementation(project(":marid-common"))
  implementation(project(":marid-moans"))

  implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json-jvm", version = "1.1.0")
  implementation(group = "org.slf4j", name = "slf4j-jdk14", version = "1.7.30")

  implementation(group = "org.openjfx", name = "javafx-controls", version = openjfxVersion)
  implementation(group = "org.openjfx", name = "javafx-media", version = openjfxVersion)
  implementation(group = "org.openjfx", name = "javafx-swing", version = openjfxVersion)

  val curOsName = System.getProperty("os.name").toLowerCase()
  val curOs = when {
    curOsName.contains("win") -> "win"
    curOsName.contains("mac") -> "mac"
    else -> "linux"
  }

  compileOnly(group = "org.openjfx", name = "javafx-controls", version = openjfxVersion, classifier = curOs)
  compileOnly(group = "org.openjfx", name = "javafx-media", version = openjfxVersion, classifier = curOs)
  compileOnly(group = "org.openjfx", name = "javafx-base", version = openjfxVersion, classifier = curOs)
  compileOnly(group = "org.openjfx", name = "javafx-swing", version = openjfxVersion, classifier = curOs)
  compileOnly(group = "org.openjfx", name = "javafx-graphics", version = openjfxVersion, classifier = curOs)
}

