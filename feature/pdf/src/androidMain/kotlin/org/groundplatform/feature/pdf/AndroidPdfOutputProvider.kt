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

private const val REPORTS_SUBDIR = "reports"

// TODO: Add equivalent iOS implementation
// Issue URL: https://github.com/google/ground-android/issues/3775
class AndroidPdfOutputProvider(private val context: Context) : PdfOutputProvider {

  private val reportsDir
    get() = File(context.cacheDir, REPORTS_SUBDIR)

  override fun newFilePath(name: String): String {
    val outputDir = reportsDir.apply { mkdirs() }
    return File(outputDir, "$name.pdf").absolutePath
  }

  override fun exists(name: String): Boolean = File(reportsDir, "$name.pdf").exists()

  override fun listFiles(): List<PdfOutputProvider.CachedPdf> =
    reportsDir
      .listFiles { f -> f.isFile && f.extension == "pdf" }
      ?.map { PdfOutputProvider.CachedPdf(it.absolutePath, it.lastModified()) } ?: emptyList()

  override fun deleteReport(path: String) {
    File(path).delete()
  }
}
