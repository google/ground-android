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
package org.groundplatform.feature.pdf.helpers

import kotlinx.coroutines.Dispatchers
import org.groundplatform.feature.pdf.PdfExportService
import org.groundplatform.feature.pdf.PdfImageProvider
import org.groundplatform.feature.pdf.PdfOutputProvider
import org.groundplatform.feature.pdf.PdfRenderer
import org.groundplatform.feature.pdf.PdfReportLauncher
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument
import org.groundplatform.feature.pdf.render.image.PdfImageSet

@Suppress("UseDataClass")
class FakePdfExportService(val outputPath: String = "/tmp/report.pdf") {
  var renderError: Throwable? = null

  var openedPath: String? = null
    private set

  var sharedPath: String? = null
    private set

  var imagesReleased: Boolean = false
    private set

  val deletedPaths: MutableList<String> = mutableListOf()

  val service: PdfExportService =
    PdfExportService(
      imageProvider =
        object : PdfImageProvider {
          override suspend fun load(qrContent: String?, photoFilenames: Set<String>) =
            PdfImageSet(images = emptyMap(), onRelease = { imagesReleased = true })
        },
      renderer =
        object : PdfRenderer {
          override suspend fun render(
            document: SubmissionPdfDocument,
            images: PdfImageSet,
            outputPath: String,
          ) {
            renderError?.let { throw it }
          }
        },
      outputProvider =
        object : PdfOutputProvider {
          override fun newFilePath(name: String) = outputPath

          override fun exists(name: String) = false

          override fun listFiles() = emptyList<PdfOutputProvider.CachedPdf>()

          override fun deleteReport(path: String) {
            deletedPaths.add(path)
          }
        },
      launcher =
        object : PdfReportLauncher {
          override fun share(path: String) {
            sharedPath = path
          }

          override fun open(path: String) {
            openedPath = path
          }
        },
      coroutineDispatcher = Dispatchers.Unconfined,
    )
}
