rootProject.name = "brunhild"

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage") repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
  }
}

include(
  "brunhild-base",
  "brunhild-cli",
)
