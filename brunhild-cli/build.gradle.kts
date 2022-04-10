plugins { application }
application.mainClass.set("org.brunhild.cli.Main")

dependencies {
  val deps: java.util.Properties by rootProject.ext
  api("org.antlr", "antlr4-runtime", version = deps.getProperty("version.antlr"))
  implementation(project(":brunhild-base"))
}

val genDir = "src/main/gen"
sourceSets["main"].java.srcDir(file(genDir))
idea.module {
  sourceDirs.add(file(genDir))
}
