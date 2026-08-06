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

import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import org.groundplatform.feature.pdf.render.PdfLine
import org.groundplatform.feature.pdf.render.PdfOffset
import org.groundplatform.feature.pdf.render.PdfRect

/**
 * Pre-computed layout for the line scale overlaid on the map snapshot: a horizontal line showing a
 * fixed ground distance, with that distance labeled above it. It lives inside the image's
 * bottom-right corner.
 *
 * @param line The scale line itself, running left to right.
 * @param labelOffset Top-left position of the distance label, laid out centered horizontally over
 *   the line.
 */
internal data class ScaleBarLayout(val line: PdfLine, val labelOffset: PdfOffset) {
  /**
   * A scale bar sized for one particular snapshot, before it has been placed on the page.
   *
   * @param lengthPoints How long the bar runs on the page.
   * @param label The distance it spans, already formatted (e.g. "200 m").
   */
  data class Bar(val lengthPoints: Float, val label: String)

  companion object {
    const val MARGIN = 8f
    const val LABEL_GAP = 1f
    private const val MAX_WIDTH_FRACTION = 0.25f
    private val ROUND_MULTIPLES = listOf(1.0, 2.0, 5.0)

    fun measure(groundWidthMeters: Double, imageWidthPoints: Float): Bar? {
      val metersPerPoint =
        (groundWidthMeters / imageWidthPoints).takeIf { it.isFinite() && it > 0.0 } ?: return null
      val distanceMeters = roundDistanceDown(imageWidthPoints * MAX_WIDTH_FRACTION * metersPerPoint)
      return distanceMeters?.let {
        Bar(lengthPoints = (it / metersPerPoint).toFloat(), label = formatDistance(it))
      }
    }

    fun compute(imageFrame: PdfRect, lengthPoints: Float, labelHeight: Float): ScaleBarLayout {
      val right = imageFrame.right - MARGIN
      val left = right - lengthPoints
      val y = imageFrame.bottom - MARGIN
      return ScaleBarLayout(
        line = PdfLine(left, y, right, y),
        labelOffset = PdfOffset(left, y - LABEL_GAP - labelHeight),
      )
    }

    private fun roundDistanceDown(meters: Double): Double? {
      if (meters < 1) return null
      val magnitude = 10.0.pow(floor(log10(meters)))
      return ROUND_MULTIPLES.map { it * magnitude }.last { it <= meters }
    }

    private fun formatDistance(meters: Double): String =
      if (meters >= 1000) "${(meters / 1000).roundToLong()} km" else "${meters.roundToLong()} m"
  }
}
