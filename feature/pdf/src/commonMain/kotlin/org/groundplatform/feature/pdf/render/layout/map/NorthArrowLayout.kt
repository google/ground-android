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
package org.groundplatform.feature.pdf.render.layout.map

import org.groundplatform.feature.pdf.render.PdfOffset
import org.groundplatform.feature.pdf.render.PdfRect

/**
 * Pre-computed layout for the north arrow overlaid on the map snapshot: an "N" label above a solid
 * arrow, at the image's top-right corner. The snapshots are always captured north-up, so the arrow
 * direction is fixed.
 *
 * @param labelOffset Top-left position of the "N" label, laid out centered within [LABEL_WIDTH].
 * @param arrow The arrow triangle, listed clockwise from the tip: tip, right base corner, left base
 *   corner.
 */
internal data class NorthArrowLayout(val labelOffset: PdfOffset, val arrow: List<PdfOffset>) {
  companion object {
    const val LABEL = "N"
    const val LABEL_WIDTH = 14
    const val ARROW_WIDTH = 11f
    const val ARROW_HEIGHT = 20f

    /** Gap between the map's edges and the arrow block. */
    const val MARGIN = 8f

    /** Vertical gap between the label and the arrow's tip. */
    const val LABEL_GAP = 1f

    fun compute(imageFrame: PdfRect, labelHeight: Float): NorthArrowLayout {
      val halfWidth = ARROW_WIDTH / 2f
      val centerX = imageFrame.right - MARGIN - halfWidth
      val top = imageFrame.y + MARGIN
      val tipY = top + labelHeight + LABEL_GAP
      val baseY = tipY + ARROW_HEIGHT
      return NorthArrowLayout(
        labelOffset = PdfOffset(centerX - LABEL_WIDTH / 2f, top),
        arrow =
          listOf(
            PdfOffset(centerX, tipY),
            PdfOffset(centerX + halfWidth, baseY),
            PdfOffset(centerX - halfWidth, baseY),
          ),
      )
    }
  }
}
