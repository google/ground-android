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

/** Tracks the current vertical draw position on a page and the space reserved for the footer. */
internal class PdfCursor(
  private val pageHeight: Int = PdfConfig.PAGE_HEIGHT,
  private val margin: Int = PdfConfig.MARGIN,
) {
  /** Space kept clear above the bottom margin for the footer; set once the footer is known. */
  var footerReserve: Float = 0f

  var y: Float = margin.toFloat()
    private set

  fun reset() {
    y = margin.toFloat()
  }

  fun moveTo(absoluteY: Float) {
    y = absoluteY
  }

  fun advance(delta: Float) {
    y += delta
  }

  /** Whether a block of the given [height] still fits above the footer reserve on this page. */
  fun fits(height: Float): Boolean = y + height <= pageHeight - margin - footerReserve
}
