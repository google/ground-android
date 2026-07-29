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

import android.graphics.RectF
import android.text.StaticLayout
import org.groundplatform.feature.pdf.render.image.PdfImage

/** Abstraction for drawing onto a PDF page. */
internal interface PdfCanvas {
  fun startPage(pageNumber: Int)

  fun finishPage()

  fun drawStaticLayout(layout: StaticLayout, x: Float, y: Float)

  fun drawImage(image: PdfImage, frame: RectF, smoothScaling: Boolean)

  fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float)

  fun drawMapOverlay(overlay: MapOverlay, darkBasemap: Boolean)
}

/** Used during the page-counting phase. Drops every drawing call. */
internal object MeasurementPdfCanvas : PdfCanvas {
  override fun startPage(pageNumber: Int) = Unit

  override fun finishPage() = Unit

  override fun drawStaticLayout(layout: StaticLayout, x: Float, y: Float) = Unit

  override fun drawImage(image: PdfImage, frame: RectF, smoothScaling: Boolean) = Unit

  override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) = Unit

  override fun drawMapOverlay(overlay: MapOverlay, darkBasemap: Boolean) = Unit
}
