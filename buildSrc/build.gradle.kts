import java.util.*

plugins {
  java
  groovy
  antlr
}

repositories { mavenCentral() }

val rootDir = projectDir.parentFile!!
val genDir = rootDir.resolve("brunhild-cli/src/main/gen")

tasks.withType<AntlrTask>().configureEach antlr@{
  outputDirectory = genDir
  val packageName = "org.brunhild.parser"
  val libPath = genDir.resolve(packageName.replace('.', '/')).absoluteFile
  doFirst { libPath.mkdirs() }
  arguments.addAll(
    listOf(
      "-package", packageName,
      "-no-listener",
      "-lib", "$libPath",
    ),
  )
}

dependencies {
  val deps = Properties()
  deps.load(rootDir.resolve("gradle/deps.properties").reader())
  antlr("org.antlr", "antlr4", deps.getProperty("version.antlr"))
}
