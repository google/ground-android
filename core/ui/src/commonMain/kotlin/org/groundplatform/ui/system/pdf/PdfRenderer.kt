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

import org.groundplatform.ui.model.SubmissionPdfDocument
import org.groundplatform.ui.system.pdf.image.PdfImageSet

/**
 * Rasterises a [SubmissionPdfDocument] to a PDF file. Each platform should use its native text
 * layout and PDF APIs to handle wrapping, pagination, and drawing. Writes the result to the
 * provided output path.
 */
interface PdfRenderer {
  suspend fun render(document: SubmissionPdfDocument, images: PdfImageSet, outputPath: String)
}
