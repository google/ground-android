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
package org.groundplatform.ui.resources

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.fail
import org.w3c.dom.Element

/**
 * Validates the Compose string resources across locales. Android lint covers this for res/values,
 * but these are Compose Multiplatform resources, so nothing else guards them.
 */
class ComposeStringResourcesTest {

  // Matches positional (%1$s) and non-positional (%s, %d) format specifiers.
  private val placeholderRegex = Regex("""%(\d+\$)?[a-zA-Z]""")

  private val composeResourcesDir =
    File("src/commonMain/composeResources").also {
      require(it.isDirectory) { "Compose resources not found at ${it.absolutePath}" }
    }

  @Test
  fun `validate translations`() {
    val default = parseStrings(File(composeResourcesDir, "values/strings.xml"))

    val issues = buildList {
      for (localeDir in localeDirs()) {
        val localized = parseStrings(File(localeDir, "strings.xml"))
        val locale = localeDir.name

        val missing = (default.keys - localized.keys).sorted()
        if (missing.isNotEmpty()) {
          add("[$locale] missing keys: ${missing.joinToString()}")
        }

        for ((key, value) in localized) {
          val expected = default[key] ?: continue
          if (placeholders(expected) != placeholders(value)) {
            add(
              "[$locale] \"$key\" placeholder mismatch: " +
                "expected ${placeholders(expected)} but found ${placeholders(value)}"
            )
          }
        }
      }
    }

    if (issues.isNotEmpty()) {
      fail("Compose string resource issues:\n" + issues.joinToString("\n"))
    }
  }

  private fun placeholders(value: String): List<String> =
    placeholderRegex.findAll(value).map { it.value }.sorted().toList()

  private fun localeDirs(): List<File> =
    composeResourcesDir
      .listFiles { file -> file.isDirectory && file.name.startsWith("values-") }
      ?.sortedBy { it.name } ?: emptyList()

  private fun parseStrings(file: File): Map<String, String> {
    if (!file.exists()) return emptyMap()
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    val nodes = doc.getElementsByTagName("string")
    return (0 until nodes.length).associate {
      val element = nodes.item(it) as Element
      element.getAttribute("name") to element.textContent
    }
  }
}
