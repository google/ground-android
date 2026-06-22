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

import android.graphics.Bitmap
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument
import org.groundplatform.feature.pdf.render.image.PdfImage
import org.groundplatform.feature.pdf.render.image.PdfImageSet
import org.groundplatform.feature.pdf.render.image.PdfImageSet.ImageRef
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfWriterTest {

  @Test
  fun `measurement and draw passes emit the same page count`() {
    val measuredPages = renderPageCount(totalPages = null)

    val drawnPages = renderPageCount(totalPages = measuredPages)

    assertTrue(measuredPages > 1)
    assertEquals(measuredPages, drawnPages)
  }

  @Test
  fun `does not open a page for a document with no qr and no rows`() {
    val canvas = FakePdfCanvas()

    newPdfWriter(EMPTY_DOCUMENT, PdfImageSet(emptyMap()), canvas).drawDocument(EMPTY_DOCUMENT)

    assertEquals(0, canvas.startedPages.size)
    assertEquals(0, canvas.finishedPages)
  }

  @Test
  fun `opens and closes exactly one page for a single-page document`() {
    val canvas = renderDocument(SINGLE_PAGE_DOCUMENT)

    assertEquals(listOf(1), canvas.startedPages)
    assertEquals(1, canvas.finishedPages)
  }

  @Test
  fun `draws the header values on the page`() {
    val canvas = renderDocument(SINGLE_PAGE_DOCUMENT)

    assertTrue(canvas.drawnText.contains(HEADER.surveyName))
    assertTrue(canvas.drawnText.contains(HEADER.jobName))
    assertTrue(canvas.drawnText.contains(HEADER.timestamp))
  }

  @Test
  fun `draws the footer text on the page`() {
    val canvas = renderDocument(SINGLE_PAGE_DOCUMENT)

    assertTrue(
      canvas.drawnText.contains(
        "${FOOTER.dataCollectorLabel}: ${FOOTER.dataCollectorName}, ${FOOTER.userEmail}"
      )
    )
  }

  @Test
  fun `draws the header and footer on every page`() {
    val canvas = FakePdfCanvas()
    val pdfWriter =
      newPdfWriter(TEST_PDF_DOCUMENT, PdfImageSet(emptyMap()), canvas, totalPages = null)

    pdfWriter.drawDocument(TEST_PDF_DOCUMENT)

    assertTrue(pdfWriter.pageCount > 1)
    assertEquals(pdfWriter.pageCount, canvas.drawnText.count { it == HEADER.surveyName })
    assertEquals(
      pdfWriter.pageCount,
      canvas.drawnText.count {
        it == "${FOOTER.dataCollectorLabel}: ${FOOTER.dataCollectorName}, ${FOOTER.userEmail}"
      },
    )
  }

  @Test
  fun `draws the qr image and caption when a qr image is provided`() {
    val qr = pdfImage()
    val canvas = renderDocument(SINGLE_PAGE_DOCUMENT, pdfImageSet(qr = qr))

    assertTrue(canvas.drawnImages.any { it.bitmap === qr.bitmap })
    assertTrue(canvas.drawnText.contains(QR_BLOCK.scanCaption))
  }

  @Test
  fun `skips the qr block when no qr image is provided`() {
    val canvas = renderDocument(SINGLE_PAGE_DOCUMENT, pdfImageSet(qr = null))

    assertFalse(canvas.drawnText.contains(QR_BLOCK.scanCaption))
  }

  @Test
  fun `draws text answers as text layouts`() {
    val canvas = renderDocument(SINGLE_PAGE_DOCUMENT)

    assertTrue(canvas.drawnText.contains(SINGLE_PAGE_DOCUMENT.table.rows[0].question))
    assertTrue(
      canvas.drawnText.contains(
        (SINGLE_PAGE_DOCUMENT.table.rows[0].answer as SubmissionPdfDocument.Answer.Text)
          .lines
          .first()
      )
    )
    assertTrue(canvas.drawnText.contains(SINGLE_PAGE_DOCUMENT.table.rows[1].question))
  }

  @Test
  fun `draws photo answers as images`() {
    val photo = pdfImage()
    val canvas =
      renderDocument(SINGLE_PAGE_DOCUMENT, pdfImageSet(photos = mapOf("photo.jpg" to photo)))

    assertTrue(canvas.drawnImages.any { it.bitmap === photo.bitmap })
  }

  @Test
  fun `does not draw a photo answer when its image is missing`() {
    val canvas = renderDocument(SINGLE_PAGE_DOCUMENT, pdfImageSet(photos = emptyMap()))

    assertTrue(canvas.drawnImages.isEmpty())
  }

  @Test
  fun `includes the page number in the footer when totalPages is set`() {
    val canvas = renderDocument(SINGLE_PAGE_DOCUMENT, totalPages = 1)

    assertTrue(canvas.drawnText.contains("1/1"))
  }

  @Test
  fun `omits the page number from the footer when totalPages is null`() {
    val canvas = renderDocument(SINGLE_PAGE_DOCUMENT, totalPages = null)

    assertFalse(canvas.drawnText.any { it.contains("/") })
  }

  @Test
  fun `draws a top border on only the first table row of a page`() {
    val canvas = renderDocument(SINGLE_PAGE_DOCUMENT)

    // SINGLE_PAGE_DOCUMENT has 2 rows on one page: the first gets a top border, the second doesn't.
    assertEquals(2, canvas.drawnLines.count { it.startX == it.endX })
    assertEquals(1, canvas.topBorderCount())
  }

  @Test
  fun `draws a fresh top border on the first row of every page`() {
    val canvas = FakePdfCanvas()
    val pdfWriter =
      newPdfWriter(TEST_PDF_DOCUMENT, PdfImageSet(emptyMap()), canvas, totalPages = null)

    pdfWriter.drawDocument(TEST_PDF_DOCUMENT)

    // Every page resets the flag, so each page's first row draws exactly 1 top border.
    assertTrue(pdfWriter.pageCount > 1)
    assertEquals(pdfWriter.pageCount, canvas.topBorderCount())
  }

  @Test
  fun `skips the table when there are no rows`() {
    val tableless =
      SINGLE_PAGE_DOCUMENT.copy(table = SINGLE_PAGE_DOCUMENT.table.copy(rows = emptyList()))

    val canvas = renderDocument(tableless, pdfImageSet(qr = pdfImage()))

    assertEquals(listOf(1), canvas.startedPages)
    assertFalse(canvas.drawnText.contains(TABLE.submissionLabel))
  }

  private fun renderDocument(
    document: SubmissionPdfDocument,
    images: PdfImageSet = pdfImageSet(qr = pdfImage()),
    totalPages: Int? = 1,
  ): FakePdfCanvas =
    FakePdfCanvas().also { newPdfWriter(document, images, it, totalPages).drawDocument(document) }

  private fun renderPageCount(totalPages: Int?): Int =
    newPdfWriter(TEST_PDF_DOCUMENT, PdfImageSet(emptyMap()), MeasurementPdfCanvas, totalPages)
      .apply { drawDocument(TEST_PDF_DOCUMENT) }
      .pageCount

  private fun newPdfWriter(
    document: SubmissionPdfDocument,
    images: PdfImageSet,
    canvas: PdfCanvas,
    totalPages: Int? = null,
  ): PdfWriter =
    PdfWriter(
      pdfCanvas = canvas,
      images = images,
      totalPages = totalPages,
      header = document.header,
      footer = document.footer,
    )

  private fun FakePdfCanvas.topBorderCount(): Int {
    // This counts the rows as each row draws exactly 1 vertical divider
    val rowCount = drawnLines.count { it.startX == it.endX }
    val horizontalLines = drawnLines.count { it.startY == it.endY }
    return horizontalLines - rowCount
  }

  private fun pdfImage(): PdfImage = PdfImage(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))

  private fun pdfImageSet(
    qr: PdfImage? = null,
    photos: Map<String, PdfImage> = emptyMap(),
  ): PdfImageSet =
    PdfImageSet(
      buildMap {
        qr?.let { put(ImageRef.Qr, it) }
        photos.forEach { (name, image) -> put(ImageRef.Photo(name), image) }
      }
    )

  private companion object {
    val HEADER =
      SubmissionPdfDocument.Header(
        surveyLabel = "Survey",
        surveyName = "Survey name",
        jobLabel = "Job",
        jobName = "Job name",
        timestamp = "timestamp",
      )
    val FOOTER =
      SubmissionPdfDocument.Footer(
        dataCollectorLabel = "Collector",
        dataCollectorName = "John Doe",
        userEmail = "user@gmail.com",
      )

    val QR_BLOCK = SubmissionPdfDocument.QrBlock(scanCaption = "Scan")

    val TABLE =
      SubmissionPdfDocument.Table(
        submissionLabel = "Submission",
        loiName = "Plot 42",
        rows = emptyList(),
      )

    val EMPTY_DOCUMENT =
      SubmissionPdfDocument(
        header = HEADER,
        qrBlock = QR_BLOCK,
        footer = FOOTER,
        table = TABLE,
      )

    val SINGLE_PAGE_DOCUMENT =
      SubmissionPdfDocument(
        header = HEADER,
        qrBlock = QR_BLOCK,
        footer = FOOTER,
        table =
          TABLE.copy(
            rows =
              listOf(
                SubmissionPdfDocument.Row(
                  question = "What is your name?",
                  answer = SubmissionPdfDocument.Answer.Text(listOf("John")),
                ),
                SubmissionPdfDocument.Row(
                  question = "Take a picture of a tree",
                  answer = SubmissionPdfDocument.Answer.Photo(remoteFilename = "photo.jpg"),
                ),
              )
          ),
      )

    val TEST_PDF_DOCUMENT =
      SubmissionPdfDocument(
        header = HEADER,
        qrBlock = QR_BLOCK,
        footer = FOOTER,
        table =
          TABLE.copy(
            rows =
              List(200) { index ->
                SubmissionPdfDocument.Row(
                  question = "Question $index",
                  answer = SubmissionPdfDocument.Answer.Text(listOf("Answer $index")),
                )
              }
          ),
      )
  }
}
