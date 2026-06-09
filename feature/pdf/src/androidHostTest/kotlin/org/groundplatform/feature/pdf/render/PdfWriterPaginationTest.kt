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
package org.groundplatform.feature.pdf.render

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument
import org.groundplatform.feature.pdf.render.image.PdfImageSet
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfWriterPaginationTest {

  @Test
  fun `measurement and draw passes emit the same page count`() {
    val measuredPages = renderPageCount(totalPages = null)

    val drawnPages = renderPageCount(totalPages = measuredPages)

    assertTrue(measuredPages > 1)
    assertEquals(measuredPages, drawnPages)
  }

  private fun renderPageCount(totalPages: Int?): Int =
    newPdfWriter(TEST_PDF_DOCUMENT, totalPages).apply { drawDocument(TEST_PDF_DOCUMENT) }.pageCount

  private fun newPdfWriter(document: SubmissionPdfDocument, totalPages: Int?): PdfWriter =
    PdfWriter(
      pdfCanvas = MeasurementPdfCanvas,
      images = PdfImageSet(emptyMap()),
      totalPages = totalPages,
      header = document.header,
      footer = document.footer,
    )

  private companion object {
    val TEST_PDF_DOCUMENT =
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
            dataCollectorName = "John Doe",
            userEmail = "user@gmail.com",
          ),
        table =
          SubmissionPdfDocument.Table(
            submissionLabel = "Submission",
            loiName = "Plot 42",
            rows =
              List(200) { index ->
                SubmissionPdfDocument.Row(
                  question = "Question $index",
                  answer = SubmissionPdfDocument.Answer.Text(listOf("Answer $index")),
                )
              },
          ),
      )
  }
}
