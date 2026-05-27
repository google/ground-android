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
package org.groundplatform.ui.system.pdf

import android.graphics.pdf.PdfDocument
import java.io.File
import org.groundplatform.ui.model.SubmissionPdfDocument
import org.groundplatform.ui.system.pdf.image.PdfImageSet

/**
 * Android [PdfRenderer] for a [SubmissionPdfDocument]. The drawing of each section lives in
 * [PdfWriter].
 */
class AndroidPdfRenderer : PdfRenderer {

  override suspend fun render(
    document: SubmissionPdfDocument,
    images: PdfImageSet,
    outputPath: String,
  ) {
    // First pass counts the pages so the footer can show "page/total"; the second renders for real.
    // Pagination is identical across passes since the page number adds no height to the footer
    // line.
    val totalPages = measurePageCount(document, images)
    val pdf = PdfDocument()
    try {
      PdfWriter(pdf, images, totalPages).draw(document)
      File(outputPath).outputStream().use { pdf.writeTo(it) }
    } finally {
      pdf.close()
    }
  }

  private fun measurePageCount(document: SubmissionPdfDocument, images: PdfImageSet): Int {
    val pdf = PdfDocument()
    return try {
      PdfWriter(pdf, images).draw(document).pageCount
    } finally {
      pdf.close()
    }
  }

  private fun PdfWriter.draw(document: SubmissionPdfDocument): PdfWriter = apply {
    setHeader(document.header)
    setFooter(document.footer)
    drawQrBlock(document.qrBlock)
    drawTable(document.table)
    finalizePage()
  }
}
