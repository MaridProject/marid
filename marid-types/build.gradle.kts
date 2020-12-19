plugins {
  `java-library`
}

dependencies {
  api(group = "org.eclipse.jdt", name = "org.eclipse.jdt.core", version = "3.24.0")
  implementation(project(":marid-common"))
  testImplementation(project(":marid-test"))
}