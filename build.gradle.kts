plugins {
  kotlin("jvm").version("1.4.21").apply(false)
  id("net.kyori.indra.license-header").version("1.2.1").apply(false)
}

group = "org.example"
version = "1.0-SNAPSHOT"

val javaVersion = JavaVersion.VERSION_15

subprojects {
  apply(plugin = "java")
  apply(plugin = "net.kyori.indra.license-header")

  repositories {
    mavenCentral()
    jcenter()
  }

  configure<JavaPluginConvention> {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.javaParameters = true
    kotlinOptions.jvmTarget = javaVersion.toString()
  }

  dependencies {
    "compileOnly"(group = "org.jetbrains", name = "annotations", version = "20.1.0")
  }
}