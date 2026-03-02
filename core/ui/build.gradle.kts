import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.android.lint)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

kotlin {
  jvmToolchain(libs.versions.jvmToolchainVersion.get().toInt())
  androidLibrary {
    namespace = "org.groundplatform.core.ui"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    minSdk = libs.versions.androidMinSdk.get().toInt()
  }

  val xcfName = "GroundUI"
  val xcf = XCFramework(xcfName)

  listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework {
      baseName = xcfName
      xcf.add(this)
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui)
        implementation(libs.compose.ui.tooling.preview)
        implementation(libs.compose.components.resources)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.qrose)
      }
    }

    commonTest { dependencies { implementation(libs.kotlin.test) } }

    androidMain { dependencies { implementation(libs.compose.ui.tooling.preview) } }

    iosMain { dependencies {} }
  }
}

dependencies { androidRuntimeClasspath(libs.compose.ui.tooling.preview) }
