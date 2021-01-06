plugins {
  kotlin("jvm")
}

dependencies {
  api(group = "com.google.jimfs", name = "jimfs", version = "1.1")
  api(kotlin("reflect"))
  api(kotlin("compiler"))
  api(project(":marid-common"))
  testApi(project(":marid-test"))
}