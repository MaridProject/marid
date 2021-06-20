plugins {
  kotlin("jvm")
}

val jupiterVersion = "5.7.2"

dependencies {
  api(group = "org.junit.jupiter", name = "junit-jupiter-params", version = jupiterVersion)
  api(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = jupiterVersion)
  api(project(":marid-common"))
  api(kotlin("stdlib"))
  api(kotlin("test-junit5"))
}