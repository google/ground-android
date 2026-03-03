plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  // We do not add an Android target here because this is a pure domain module.
  // Adding an Android target would require the Android Gradle Plugin (AGP) and SDK configuration,
  // which would couple the domain layer to platform-specific infrastructure.
  // iOS targets are included because Kotlin can compile to iOS without any platform plugin.
  jvm()
  jvmToolchain(libs.versions.jvmToolchainVersion.get().toInt())

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.collections.immutable)
      }
    }

    commonTest { dependencies { implementation(libs.kotlin.test) } }
  }
}
