plugins {
  kotlin("jvm")
}

dependencies {
  testApi(group = "it.unibo.tuprolog", name = "solve-streams-jvm", version = "0.15.2")
  testApi(project(":marid-test"))
}