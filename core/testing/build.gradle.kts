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
  androidLibrary {
    namespace = "org.groundplatform.core.testing"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    minSdk = libs.versions.androidMinSdk.get().toInt()
  }

  jvm()

  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":core:domain"))
        implementation(project(":core:ui"))
        implementation(libs.kotlin.stdlib)
        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.compose.components.resources)
      }
    }
  }
}
