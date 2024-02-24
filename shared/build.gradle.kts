/*
 * Copyright 2024 Google LLC
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

import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegen
import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegenPlugin

plugins {
  // Android library plugin.
  id("com.android.library")
  // Kotlin plugins for Gradle.
  id("kotlin-android")
}

android {
  namespace = "com.google.ground.shared.schema"
  // TODO(#2206): Manage compileSdk version centrally.
  compileSdk = 34
  sourceSets { getByName("main").java.srcDirs("build/generated-sources/kotlin") }
}

// TODO(#2206): Manage version centrally (e.g., via rootProject.jvmToolchainVersion).
kotlin { jvmToolchain(17) }

// TODO(#2206): Manage version centrally.
dependencies { implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.9.20") }

buildscript {
  repositories { mavenCentral() }
  dependencies { classpath("net.pwall.json:json-kotlin-gradle:0.102") }
}

apply<JSONSchemaCodegenPlugin>()

configure<JSONSchemaCodegen> {
  configFile.set(file("json-schema-codegen/config.json")) // if not in the default location
  //    inputs { inputFile(file("src/main/resources/schema")) }
  inputs {
    val base = "../../ground-platform/schema/src"
    inputFile(file("$base/loi-document.schema.json"))
    inputFile(file("$base/geometry-data.schema.json"))
    inputFile(file("$base/point-data.schema.json"))
    inputFile(file("$base/polygon-data.schema.json"))
    inputFile(file("$base/multi-polygon-data.schema.json"))
    inputFile(file("$base/linear-ring-data.schema.json"))
    inputFile(file("$base/audit-info.schema.json"))
    inputFile(file("$base/submission-document.schema.json"))
  }
  outputDir.set(file("build/generated-sources/kotlin"))
  packageName.set("com.google.ground.shared.schema")
}
