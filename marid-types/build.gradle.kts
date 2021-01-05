plugins {
  kotlin("jvm")
}

dependencies {
  api(group = "com.google.jimfs", name = "jimfs", version = "1.1")
  api(kotlin("reflect"))
  testApi(project(":marid-test"))
}