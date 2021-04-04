plugins {
  kotlin("jvm")
}

val graalVmVersion = "21.0.0.2"

dependencies {
  implementation(group = "org.slf4j", name = "slf4j-jdk14", version = "1.7.30")
  implementation(group = "org.graalvm.js", name = "js", version = graalVmVersion)
  implementation(group = "org.graalvm.js", name = "js-scriptengine", version = graalVmVersion)
  testApi(project(":marid-test"))
}