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
}

kotlin {
  jvmToolchain(libs.versions.jvmToolchainVersion.get().toInt())
  android {
    namespace = "org.groundplatform.feature.pdf"
    compileSdk {
      version =
        release(libs.versions.androidCompileSdk.get().toInt()) {
          minorApiLevel = libs.versions.androidCompileSdkMinor.get().toInt()
        }
    }
    minSdk = libs.versions.androidMinSdk.get().toInt()
    withHostTest {}
  }

  val xcfName = "GroundFeaturePdfKit"
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
        implementation(project(":core:ui"))
        implementation(libs.kotlin.stdlib)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.serialization.json)
      }
    }

    commonTest {
      dependencies {
        implementation(project(":core:testing"))
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.exifinterface)
        implementation(libs.compose.ui)
      }
    }

    val androidHostTest by getting {
      dependencies {
        implementation(libs.junit)
        implementation(libs.robolectric)
      }
    }
  }
}
