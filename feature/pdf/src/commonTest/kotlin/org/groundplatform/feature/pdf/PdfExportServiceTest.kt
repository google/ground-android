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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument
import org.groundplatform.feature.pdf.render.image.PdfImageSet

class PdfExportServiceTest {

  private val path = "/tmp/report.pdf"
  private val deletedPaths = mutableListOf<String>()
  private var imagesReleased = false
  private var sharedPath: String? = null
  private var openedPath: String? = null

  @Test
  fun `deletes corrupted file and rethrows when rendering fails`() = runTest {
    val failure = RuntimeException("out of memory")

    val thrown =
      assertFailsWith<RuntimeException> {
        service(renderError = failure).export(request, PdfExportService.Action.Open)
      }

    assertEquals(failure, thrown)
    assertEquals(listOf(path), deletedPaths)
    assertTrue(imagesReleased)
  }

  @Test
  fun `does not delete file and launches when rendering succeeds`() = runTest {
    service().export(request, PdfExportService.Action.Share)

    assertTrue(deletedPaths.isEmpty())
    assertEquals(path, sharedPath)
    assertTrue(imagesReleased)
  }

  private fun service(renderError: Throwable? = null) =
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
          override fun newFilePath(name: String) = path

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

  private companion object {
    val document =
      SubmissionPdfDocument(
        header =
          SubmissionPdfDocument.Header(
            surveyLabel = "Survey",
            surveyName = "Survey name",
            jobLabel = "Job",
            jobName = "Job name",
            timestamp = "timestamp",
          ),
        qrBlock = SubmissionPdfDocument.QrBlock(scanCaption = "Scan"),
        footer =
          SubmissionPdfDocument.Footer(
            dataCollectorLabel = "Collector",
            dataCollectorName = "Name",
            userEmail = "user@example.com",
          ),
        table =
          SubmissionPdfDocument.Table(
            submissionLabel = "Submission",
            loiName = "Loi",
            rows = emptyList(),
          ),
      )

    val request =
      PdfExportService.Request(document = document, qrContent = null, fileName = "report.pdf")
  }
}
