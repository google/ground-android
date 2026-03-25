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
  alias(libs.plugins.kotlin.serialization)
}

apply(from = "../../config/jacoco/jacoco.gradle")

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

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
      }
    }
  }
}
