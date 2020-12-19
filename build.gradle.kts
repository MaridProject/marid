plugins {
  kotlin("jvm").version("1.4.21").apply(false)
}

group = "org.example"
version = "1.0-SNAPSHOT"

val javaVersion = JavaVersion.VERSION_15

subprojects {
  apply(plugin = "java")

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
    kotlinOptions.jvmTarget = javaVersion.name
  }
}