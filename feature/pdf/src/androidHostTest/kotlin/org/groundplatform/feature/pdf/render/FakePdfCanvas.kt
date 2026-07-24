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

internal class FakePdfCanvas : PdfCanvas {
  val startedPages = mutableListOf<Int>()
  var finishedPages = 0
  val drawnText = mutableListOf<String>()
  val drawnImages = mutableListOf<PdfImage>()
  val drawnLines = mutableListOf<PdfLine>()
  val drawnOverlays = mutableListOf<Pair<MapOverlay, Boolean>>()

  override fun startPage(pageNumber: Int) {
    startedPages += pageNumber
  }

  override fun finishPage() {
    finishedPages++
  }

  override fun drawStaticLayout(layout: StaticLayout, x: Float, y: Float) {
    drawnText += layout.text.toString()
  }

  override fun drawImage(image: PdfImage, frame: RectF, smoothScaling: Boolean) {
    drawnImages += image
  }

  override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
    drawnLines += PdfLine(x1, y1, x2, y2)
  }

  override fun drawMapOverlay(overlay: MapOverlay, darkBasemap: Boolean) {
    drawnOverlays += overlay to darkBasemap
  }
}
