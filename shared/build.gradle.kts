import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegen
import net.pwall.json.kotlin.codegen.gradle.JSONSchemaCodegenPlugin

plugins { id("com.android.library") }

android {
  namespace = "com.google.ground.shared.schema"
  compileSdk = 34 // TODO: Use variable
  sourceSets { getByName("main").java.srcDirs("build/generated-sources/kotlin") }
}

// TODO: Use variable for ver
dependencies { implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.9.20") }

buildscript {
  repositories { mavenCentral() }
  dependencies { classpath("net.pwall.json:json-kotlin-gradle:0.99.1") }
}

apply<JSONSchemaCodegenPlugin>()

configure<JSONSchemaCodegen> {
  packageName.set("com.google.ground.shared.schema")
  inputs { inputFile(file("schema")) }
  // Set explicitly so that generated sources are written to build/ in this module and not in
  // project root.
  outputDir.set(file("build/generated-sources/kotlin"))
}
