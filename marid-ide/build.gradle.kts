plugins {
  application
  kotlin("jvm")
}

val openjfxVersion = "15.0.1"

dependencies {
  implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json-jvm", version = "1.0.1")

  implementation(group = "org.openjfx", name = "javafx-controls", version = openjfxVersion)
  implementation(group = "org.openjfx", name = "javafx-media", version = openjfxVersion)
  implementation(group = "org.openjfx", name = "javafx-swing", version = openjfxVersion)

  val curOsName = System.getProperty("os.name").toLowerCase()
  val curOs = when {
    curOsName.contains("win") -> "win"
    curOsName.contains("mac") -> "mac"
    else -> "linux"
  }

  implementation(group = "org.openjfx", name = "javafx-controls", version = openjfxVersion, classifier = curOs)
  implementation(group = "org.openjfx", name = "javafx-media", version = openjfxVersion, classifier = curOs)
  implementation(group = "org.openjfx", name = "javafx-base", version = openjfxVersion, classifier = curOs)
  implementation(group = "org.openjfx", name = "javafx-swing", version = openjfxVersion, classifier = curOs)
  implementation(group = "org.openjfx", name = "javafx-graphics", version = openjfxVersion, classifier = curOs)
}
