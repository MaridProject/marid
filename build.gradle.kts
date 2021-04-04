plugins {
  kotlin("jvm").version("1.4.32").apply(false)
  id("net.kyori.indra.license-header").version("1.2.1").apply(false)
}

group = "org.marid"
version = "1.0-SNAPSHOT"

val javaVersion = JavaVersion.VERSION_15

subprojects {
  apply(plugin = "java")
  apply(plugin = "net.kyori.indra.license-header")

  repositories {
    mavenCentral()
    jcenter()
  }

  afterEvaluate {
    tasks.getByName("clean") {
      dependsOn(
        tasks.getByName("updateLicenses")
      )
    }
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
    kotlinOptions.allWarningsAsErrors = true
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

  val jupiterVersion = "5.7.1"

  dependencies {
    "compileOnly"(group = "org.jetbrains", name = "annotations", version = "20.1.0")
    "testImplementation"(group = "org.junit.jupiter", name = "junit-jupiter-params", version = jupiterVersion)
    "testRuntimeOnly"(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = jupiterVersion)
  }
}