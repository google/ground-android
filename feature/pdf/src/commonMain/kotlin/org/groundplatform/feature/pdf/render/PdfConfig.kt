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

  /** Target size of the QR code block. */
  const val QR_SIZE = 200

  /** Horizontal gap between the two columns of the page header. */
  const val HEADER_COLUMN_GAP = 16

  /** Inner padding between a table cell's border and its text. */
  const val CELL_PADDING = 6

  /** Stroke width used for table and cell borders. */
  const val BORDER_WIDTH = 0.5f

  /** Vertical gap below the header before body content begins. */
  const val HEADER_BOTTOM_GAP = 28f

  /** Vertical gap above the footer separating it from body content. */
  const val FOOTER_TOP_GAP = 28f

  /** Maximum number of lines rendered for each header value. */
  const val MAX_HEADER_VALUE_LINES = 1

  /** Maximum number of lines rendered for the footer text. */
  const val MAX_FOOTER_LINES = 1

  /** Resolution in dots-per-inch used when rasterizing images for the PDF. */
  const val IMAGE_RENDER_DPI = 300f

  /** Content width between the left and right margins. */
  const val USABLE_WIDTH = PAGE_WIDTH - 2 * MARGIN

  /** Fraction of [USABLE_WIDTH] allotted to the task-label column of a table row. */
  private const val TABLE_TASK_LABEL_RATIO = 0.35f

  /** Width of the task-label column. */
  const val TABLE_TASK_COLUMN_WIDTH = (USABLE_WIDTH * TABLE_TASK_LABEL_RATIO).toInt()

  /** Width for task-label text (the task column minus its padding). */
  const val TABLE_TASK_TEXT_WIDTH = TABLE_TASK_COLUMN_WIDTH - 2 * CELL_PADDING

  /** Width for answer text (the remaining table width minus its padding). */
  const val TABLE_ANSWER_TEXT_WIDTH = USABLE_WIDTH - TABLE_TASK_COLUMN_WIDTH - 2 * CELL_PADDING

  /** Fraction of the usable page height a single photo may occupy before being scaled down. */
  private const val PHOTO_MAX_HEIGHT_RATIO = 0.35f

  /** Maximum table photo height. */
  const val PHOTO_MAX_HEIGHT = ((PAGE_HEIGHT - 2 * MARGIN) * PHOTO_MAX_HEIGHT_RATIO).toInt()

  /** Width reserved on the footer's right side for the page-number label. */
  const val PAGE_NUMBER_BAND_WIDTH = 60

  /** Minimum horizontal gap between the footer text and the page-number. */
  private const val FOOTER_PAGE_NUMBER_GAP = 8

  /** Maximum width for footer text (usable width minus the page-number and its gap) */
  const val FOOTER_TEXT_MAX_WIDTH = USABLE_WIDTH - PAGE_NUMBER_BAND_WIDTH - FOOTER_PAGE_NUMBER_GAP
}
