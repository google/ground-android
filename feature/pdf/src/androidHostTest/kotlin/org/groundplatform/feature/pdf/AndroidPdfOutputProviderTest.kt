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
package org.groundplatform.feature.pdf

import android.content.Context
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidPdfOutputProviderTest {

  private lateinit var context: Context
  private lateinit var reportsDir: File
  private lateinit var provider: AndroidPdfOutputProvider

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    reportsDir = File(context.cacheDir, PDF_SUBDIR)
    reportsDir.deleteRecursively()
    provider = AndroidPdfOutputProvider(context)
  }

  @Test
  fun `newFilePath creates the reports directory and returns a pdf path`() {
    val path = provider.newFilePath(PDF_FILE_NAME)

    assertTrue(reportsDir.isDirectory)
    assertEquals(File(reportsDir, "report.pdf").absolutePath, path)
  }

  @Test
  fun `exists reflects whether the report file is present`() {
    assertFalse(provider.exists(PDF_FILE_NAME))

    File(provider.newFilePath(PDF_FILE_NAME)).writeText(PDF_TEXT)

    assertTrue(provider.exists(PDF_FILE_NAME))
  }

  @Test
  fun `listFiles returns an empty list when there is no reports directory`() {
    assertTrue(provider.listFiles().isEmpty())
  }

  @Test
  fun `listFiles returns only pdf files`() {
    File(provider.newFilePath("a")).writeText(PDF_TEXT)
    File(provider.newFilePath("b")).writeText(PDF_TEXT)
    File(reportsDir, "notes.txt").writeText("ignore me")

    val names = provider.listFiles().map { File(it.path).name }.sorted()

    assertContentEquals(listOf("a.pdf", "b.pdf"), names)
  }

  @Test
  fun `listFiles returns the cached pdf files with the correct lastModified value`() {
    val file = File(provider.newFilePath(PDF_SUBDIR)).apply { writeText(PDF_TEXT) }
    file.setLastModified(987654321L)

    val entry = provider.listFiles().single()

    assertEquals(file.absolutePath, entry.path)
    assertEquals(987654321L, entry.lastModifiedMillis)
  }

  @Test
  fun `deleteReport removes the file at the given path`() {
    val path = provider.newFilePath(PDF_SUBDIR)
    File(path).writeText(PDF_TEXT)

    provider.deleteReport(path)

    assertFalse(File(path).exists())
  }

  @Test
  fun `pruneOldFiles deletes only reports older than a week`() {
    val now = System.currentTimeMillis()
    val fresh = File(provider.newFilePath("fresh")).apply { writeText(PDF_TEXT) }
    val stale = File(provider.newFilePath("stale")).apply { writeText(PDF_TEXT) }
    stale.setLastModified(now - 8L * 24 * 60 * 60 * 1000)

    provider.pruneOldFiles()

    assertTrue(fresh.exists())
    assertFalse(stale.exists())
  }

  private companion object {
    const val PDF_TEXT = "This is a test PDF."
    const val PDF_SUBDIR = "reports"
    const val PDF_FILE_NAME = "report"
  }
}
