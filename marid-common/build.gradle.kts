plugins {
  kotlin("jvm")
}

val mavenResolverVersion = "1.6.2"

dependencies {
  api(group = "com.google.guava", name = "guava", version = "30.1.1-jre")
  api(group = "org.apache.commons", name = "commons-lang3", version = "3.12.0")
  api(group = "org.eclipse.jdt", name = "ecj", version = "3.25.0")
  api(group = "org.apache.maven.resolver", name = "maven-resolver-impl", version = mavenResolverVersion)
  api(group = "org.apache.maven.resolver", name = "maven-resolver-transport-http", version = mavenResolverVersion)
  api(group = "org.apache.maven.resolver", name = "maven-resolver-transport-file", version = mavenResolverVersion)
  api(kotlin("reflect"))
  testApi(project(":marid-test"))
}