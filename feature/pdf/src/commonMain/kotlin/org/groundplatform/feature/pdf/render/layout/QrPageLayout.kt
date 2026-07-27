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
package org.groundplatform.feature.pdf.render.layout

import org.groundplatform.feature.pdf.render.PdfConfig.LINE_SPACING
import org.groundplatform.feature.pdf.render.PdfConfig.QR_PAGE_HEIGHT
import org.groundplatform.feature.pdf.render.PdfConfig.QR_PAGE_WIDTH
import org.groundplatform.feature.pdf.render.PdfOffset
import org.groundplatform.feature.pdf.render.PdfRect

/**
 * Pre-computed layout for the QR page: the QR code and its caption centered on a page of
 * [QR_PAGE_WIDTH] x [QR_PAGE_HEIGHT]. Compute should only be called when a QR image is available;
 * the caption is meaningless without it.
 *
 * @param qrFrame Position and size of the QR image.
 * @param captionOffset Top-left position of the caption text (centered under the QR).
 */
internal data class QrPageLayout(val qrFrame: PdfRect, val captionOffset: PdfOffset) {
  companion object {
    const val MARGIN = 16f

    /** Target size of the QR code, spanning the page width between the margins. */
    const val QR_SIZE = QR_PAGE_WIDTH - 2 * MARGIN

    fun compute(captionHeight: Float): QrPageLayout {
      val blockHeight = QR_SIZE + LINE_SPACING + captionHeight
      val top = (QR_PAGE_HEIGHT - blockHeight) / 2
      return QrPageLayout(
        qrFrame = PdfRect(MARGIN, top, QR_SIZE, QR_SIZE),
        captionOffset = PdfOffset(MARGIN, top + QR_SIZE + LINE_SPACING),
      )
    }
  }
}
