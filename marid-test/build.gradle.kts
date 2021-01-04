plugins {
  kotlin("jvm")
}

val jupiterVersion = "5.7.0"

dependencies {
  api(project(":marid-common"))
  api(kotlin("stdlib"))
  api(kotlin("test-junit5"))
  api(group = "org.junit.jupiter", name = "junit-jupiter-params", version = jupiterVersion)
  runtimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = jupiterVersion)
}