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

/**
 * Dimensional and type-scale constants shared by the Android and iOS PDF renderers. Keeping these
 * in commonMain prevents the two platforms from drifting on page size, margins, or type scale.
 *
 * All measurements are in PDF points (1/72 inch); A4 is 595 × 842.
 */
object PdfConfig {
  const val PAGE_WIDTH = 595
  const val PAGE_HEIGHT = 842
  const val MARGIN = 40
  const val HEADER_SIZE = 11f
  const val BODY_SIZE = 11f
  const val CAPTION_SIZE = 9f
  const val LINE_SPACING = 4f
  const val QR_SIZE = 200
  const val HEADER_COLUMN_GAP = 16
  const val TABLE_QUESTION_RATIO = 0.35f
  const val CELL_PADDING = 6
  const val BORDER_WIDTH = 0.5f
  const val PHOTO_MAX_HEIGHT = 360
  const val HEADER_BOTTOM_GAP = 28f
  const val FOOTER_TOP_GAP = 28f
  const val MAX_HEADER_VALUE_LINES = 2
  const val MAX_FOOTER_LINES = 2
}
