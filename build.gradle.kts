plugins {
  kotlin("jvm").version("1.4.20").apply(false)
}

group = "org.example"
version = "1.0-SNAPSHOT"

subprojects {
  repositories {
    mavenCentral()
    jcenter()
  }
}
