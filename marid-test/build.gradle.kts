plugins {
  kotlin("jvm")
}

val jupiterVersion = "5.7.0"

dependencies {
  api(project(":marid-common"))
  api(kotlin("stdlib"))
  api(kotlin("test-junit5"))
}