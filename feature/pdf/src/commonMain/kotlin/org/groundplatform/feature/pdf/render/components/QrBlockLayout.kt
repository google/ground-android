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
package org.groundplatform.feature.pdf.render.components

import org.groundplatform.feature.pdf.render.PdfConfig.LINE_SPACING
import org.groundplatform.feature.pdf.render.PdfConfig.MARGIN
import org.groundplatform.feature.pdf.render.PdfConfig.PAGE_WIDTH
import org.groundplatform.feature.pdf.render.PdfConfig.QR_SIZE
import org.groundplatform.feature.pdf.render.PdfOffset
import org.groundplatform.feature.pdf.render.PdfRect

/**
 * Pre-computed layout for the right-aligned QR code block with its caption. Compute should only be
 * called when a QR image is available; the caption is meaningless without it.
 *
 * @param qrFrame Position and size of the QR image.
 * @param captionOffset Top-left position of the caption text (centered under the QR).
 * @param captionMaxWidth Maximum width for the caption.
 * @param nextCursorY Cursor Y position after this block.
 */
internal data class QrBlockLayout(
  val qrFrame: PdfRect,
  val captionOffset: PdfOffset,
  val captionMaxWidth: Int,
  val nextCursorY: Float,
) {
  companion object {
    fun compute(top: Float, captionHeight: Float): QrBlockLayout {
      val x = (PAGE_WIDTH - MARGIN - QR_SIZE).toFloat()
      val captionTop = top + QR_SIZE + LINE_SPACING
      return QrBlockLayout(
        qrFrame = PdfRect(x, top, QR_SIZE.toFloat(), QR_SIZE.toFloat()),
        captionOffset = PdfOffset(x, captionTop),
        captionMaxWidth = QR_SIZE,
        nextCursorY = captionTop + captionHeight + LINE_SPACING * 2,
      )
    }
  }
}
