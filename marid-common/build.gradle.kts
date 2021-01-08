plugins {
  kotlin("jvm")
}

val mavenResolverVersion = "1.6.1"

dependencies {
  api(group = "com.google.guava", name = "guava", version = "30.1-jre")
  api(group = "org.apache.commons", name = "commons-lang3", version = "3.11")
  api(group = "org.apache.maven.resolver", name = "maven-resolver-impl", version = mavenResolverVersion)
  api(group = "org.apache.maven.resolver", name = "maven-resolver-transport-http", version = mavenResolverVersion)
  api(group = "org.apache.maven.resolver", name = "maven-resolver-transport-file", version = mavenResolverVersion)
  api(kotlin("reflect"))
  testApi(project(":marid-test"))
}