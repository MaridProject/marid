plugins {
  kotlin("jvm").version("1.5.21").apply(false)
  id("org.cadixdev.licenser").version("0.6.1")
}

group = "org.marid"
version = "1.0-SNAPSHOT"

val javaVersion = JavaVersion.VERSION_16

subprojects {
  apply(plugin = "java")
  apply(plugin = "org.cadixdev.licenser")

  repositories {
    mavenCentral()
  }

  afterEvaluate {
    tasks.getByName("clean") {
      dependsOn(
        tasks.getByName("updateLicenses")
      )
    }
  }

  configure<JavaPluginExtension> {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.javaParameters = true
    kotlinOptions.jvmTarget = javaVersion.toString()
    kotlinOptions.allWarningsAsErrors = false
    kotlinOptions.freeCompilerArgs = listOf(
      "-Xjvm-default=all",
      "-Xemit-jvm-type-annotations",
      "-Xstring-concat=indy-with-constants"
    )
  }

  tasks.withType<Test> {
    maxParallelForks = 1

    jvmArgs(
      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.management/sun.management=ALL-UNNAMED",
      "--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED"
    )

    testLogging {
      events = enumValues<org.gradle.api.tasks.testing.logging.TestLogEvent>().toSet()
      exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
      showExceptions = true
      showCauses = true
      showStackTraces = true
      showStandardStreams = true
      maxGranularity = 3
      minGranularity = 3
    }

    useJUnitPlatform()
  }

  configurations.all {
    resolutionStrategy.eachDependency {
      if (requested.group == "org.jetbrains" && requested.name == "annotations") {
        useVersion("21.0.1")
      }
    }
  }

  license {
    include("**/*.java")
    include("**/*.kt")
    setHeader(rootProject.file("license_header.txt"))
    newLine(false)
  }
}