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
        implementation("network.chaintech:qr-kit:${libs.versions.qrKitVersion.get()}") {
          // qr-kit's transitive dep (cmp-image-pick-n-crop) incorrectly declares
          // test dependencies as runtime deps, causing version conflicts with AGP's
          // consistent resolution.
          exclude(group = "androidx.compose.ui", module = "ui-test-junit4")
          exclude(group = "org.jetbrains.compose.ui", module = "ui-tooling")
        }
      }
    }

    commonTest { dependencies { implementation(libs.kotlin.test) } }

    androidMain { dependencies { implementation(libs.compose.ui.tooling.preview) } }

    iosMain { dependencies {} }
  }
}

dependencies { androidRuntimeClasspath(libs.compose.ui.tooling.preview) }
