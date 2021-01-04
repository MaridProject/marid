plugins {
  kotlin("jvm")
}

dependencies {
  api(group = "com.google.guava", name = "guava", version = "30.1-jre")
  api(group = "org.apache.commons", name = "commons-lang3", version = "3.11")
  testApi(project(":marid-test"))
}