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

import kotlin.math.roundToInt

internal fun fitInside(width: Int, height: Int, maxWidth: Int, maxHeight: Int): PdfItemSize {
  val scale = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height, 1f)
  return PdfItemSize(width * scale, height * scale)
}

internal fun pointsToRenderPixels(points: Float): Int =
  // 1 point = 1/72 inch (standard PDF user-space unit)
  (points / 72f * PdfConfig.IMAGE_RENDER_DPI).roundToInt()

internal data class PdfItemSize(val width: Float, val height: Float)

internal data class PdfOffset(val x: Float, val y: Float)

internal data class PdfLine(val startX: Float, val startY: Float, val endX: Float, val endY: Float)

/** Platform-agnostic rectangle defined by its top-left corner and dimensions. */
internal data class PdfRect(val x: Float, val y: Float, val width: Float, val height: Float) {
  val right: Float
    get() = x + width

  val bottom: Float
    get() = y + height
}
