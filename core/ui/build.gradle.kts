/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.android.lint)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

apply(from = "../../config/jacoco/jacoco.gradle")

compose.resources { publicResClass = true }

kotlin {
  jvmToolchain(libs.versions.jvmToolchainVersion.get().toInt())
  android {
    namespace = "org.groundplatform.core.ui"
    compileSdk {
      version =
        release(libs.versions.androidCompileSdk.get().toInt()) {
          minorApiLevel = libs.versions.androidCompileSdkMinor.get().toInt()
        }
    }
    minSdk = libs.versions.androidMinSdk.get().toInt()
    androidResources.enable = true

    withHostTest { isIncludeAndroidResources = true }
  }

  val xcfName = "GroundUiKit"
  listOf(iosArm64(), iosSimulatorArm64()).forEach {
    it.binaries.framework {
      baseName = xcfName
      isStatic = true
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":core:domain"))
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui)
        implementation(libs.compose.ui.tooling.preview)
        api(libs.compose.components.resources)
        implementation(libs.kotlinx.collections.immutable)
      }
    }

    commonTest {
      dependencies {
        implementation(project(":core:testing"))
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
      }
    }

    androidMain { dependencies { implementation(libs.google.zxing) } }

    val androidHostTest by getting {
      dependencies {
        implementation(libs.junit)
        implementation(libs.robolectric)
      }
    }

    iosMain { dependencies {} }
  }
}

dependencies { androidRuntimeClasspath(libs.compose.ui.tooling) }
