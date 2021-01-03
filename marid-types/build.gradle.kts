plugins {
  kotlin("jvm")
}

dependencies {
  api(kotlin("stdlib-jdk8"))
  api(kotlin("reflect"))
  testApi(project(":marid-test"))
}