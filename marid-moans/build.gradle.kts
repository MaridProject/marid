plugins {
  kotlin("jvm")
}

dependencies {
  api(kotlin("reflect"))
  testImplementation(project(":marid-test"))
}