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
  dependencies { classpath("net.pwall.json:json-kotlin-gradle:0.99.1") }
}

apply<JSONSchemaCodegenPlugin>()

configure<JSONSchemaCodegen> {
  packageName.set("com.google.ground.shared.schema")
  // Inputs and outputs must be specified since plugins defaults are relative to project root.
  inputs { inputFile(file("src/main/resources/schema")) }
  outputDir.set(file("build/generated-sources/kotlin"))
}
