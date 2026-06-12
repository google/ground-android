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

/**
 * Dimensional and type-scale constants shared by the Android and iOS PDF renderers. Keeping these
 * in commonMain prevents the two platforms from drifting on page size, margins, or type scale.
 *
 * Unless noted otherwise, all measurements are in PDF points (1/72 inch).
 */
internal object PdfConfig {
  /** Page width in points (A4 portrait, 210mm). */
  const val PAGE_WIDTH = 595

  /** Page height in points (A4 portrait, 297mm). */
  const val PAGE_HEIGHT = 842

  /** Page margin applied to all four edges. */
  const val MARGIN = 40

  /** Font size for title text. */
  const val TITLE_SIZE = 11f

  /** Font size body and table-cell text. */
  const val BODY_SIZE = 11f

  /** Font size for captions and metadata (header/footer) text. */
  const val CAPTION_SIZE = 9f

  /** Vertical spacing added between lines of text. */
  const val LINE_SPACING = 4f

  /** Content width between the left and right margins. */
  const val USABLE_WIDTH = PAGE_WIDTH - 2 * MARGIN

  /** Resolution in dots-per-inch used when rasterizing images for the PDF. */
  const val IMAGE_RENDER_DPI = 300f
}
