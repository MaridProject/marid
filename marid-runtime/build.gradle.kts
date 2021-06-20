plugins {
  `java-library`
}

val graalVmVersion = "21.1.0"

dependencies {
  implementation(group = "org.slf4j", name = "slf4j-jdk14", version = "1.7.30")
  implementation(group = "org.graalvm.js", name = "js", version = graalVmVersion)
  implementation(group = "org.graalvm.js", name = "js-scriptengine", version = graalVmVersion)
  testImplementation(project(":marid-test"))
}