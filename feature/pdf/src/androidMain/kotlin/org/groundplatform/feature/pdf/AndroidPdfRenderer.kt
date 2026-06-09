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

import android.graphics.pdf.PdfDocument
import java.io.File
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument
import org.groundplatform.feature.pdf.render.DocumentPdfCanvas
import org.groundplatform.feature.pdf.render.MeasurementPdfCanvas
import org.groundplatform.feature.pdf.render.PdfCanvas
import org.groundplatform.feature.pdf.render.PdfWriter
import org.groundplatform.feature.pdf.render.image.PdfImageSet

/**
 * Android [PdfRenderer] for a [SubmissionPdfDocument]. The drawing of each section lives in
 * [PdfWriter]; the [PdfCanvas] decides whether each pass writes to a real [PdfDocument] or just
 * counts pages.
 */
class AndroidPdfRenderer : PdfRenderer {

  override suspend fun render(
    document: SubmissionPdfDocument,
    images: PdfImageSet,
    outputPath: String,
  ) {
    // Measurement first so the footer can show "page/total"
    val totalPages = measurePageCount(document, images)
    val pdf = PdfDocument()
    try {
      writer(document, images, DocumentPdfCanvas(pdf), totalPages = totalPages)
        .drawDocument(document)
      File(outputPath).outputStream().use { pdf.writeTo(it) }
    } finally {
      pdf.close()
    }
  }

  private fun measurePageCount(document: SubmissionPdfDocument, images: PdfImageSet): Int =
    writer(document, images, MeasurementPdfCanvas, totalPages = null)
      .apply { drawDocument(document) }
      .pageCount

  private fun writer(
    document: SubmissionPdfDocument,
    images: PdfImageSet,
    pdfCanvas: PdfCanvas,
    totalPages: Int?,
  ): PdfWriter =
    PdfWriter(
      pdfCanvas = pdfCanvas,
      images = images,
      header = document.header,
      footer = document.footer,
      totalPages = totalPages,
    )
}
