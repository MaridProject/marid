plugins {
  kotlin("jvm")
}

val mavenResolverVersion = "1.7.0"

dependencies {
  api(group = "com.google.guava", name = "guava", version = "30.1.1-jre")
  api(group = "org.apache.commons", name = "commons-lang3", version = "3.12.0")
  api(group = "com.google.jimfs", name = "jimfs", version = "1.2")
  api(group = "org.apache.maven.resolver", name = "maven-resolver-impl", version = mavenResolverVersion)
  api(group = "org.apache.maven.resolver", name = "maven-resolver-transport-http", version = mavenResolverVersion)
  api(group = "org.apache.maven.resolver", name = "maven-resolver-transport-file", version = mavenResolverVersion)
  api(kotlin("reflect"))
  api(kotlin("compiler"))
  api(project(":marid-runtime"))
  testImplementation(project(":marid-test"))
}