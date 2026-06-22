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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import androidx.core.graphics.withTranslation
import org.groundplatform.feature.pdf.render.image.PdfImage
import org.groundplatform.feature.pdf.render.layout.TableLayout

/**
 * [PdfCanvas] that draws onto a real [PdfDocument], one page at a time. Image bitmaps are expected
 * to arrive at their on-page pixel size; the canvas does no further scaling.
 */
internal class DocumentPdfCanvas(private val pdf: PdfDocument) : PdfCanvas {
  private var currentPage: PdfDocument.Page? = null

  private val strokePaint =
    Paint().apply {
      style = Paint.Style.STROKE
      strokeWidth = TableLayout.BORDER_WIDTH
      isAntiAlias = true
    }

  private val smoothImagePaint =
    Paint().apply {
      isFilterBitmap = true
      isAntiAlias = true
      isDither = true
    }

  override fun startPage(pageNumber: Int) {
    val info =
      PdfDocument.PageInfo.Builder(PdfConfig.PAGE_WIDTH, PdfConfig.PAGE_HEIGHT, pageNumber).create()
    currentPage = pdf.startPage(info)
  }

  override fun finishPage() {
    currentPage?.also { pdf.finishPage(it) }
    currentPage = null
  }

  override fun drawStaticLayout(layout: StaticLayout, x: Float, y: Float) {
    canvas().withTranslation(x, y) { layout.draw(this) }
  }

  override fun drawImage(image: PdfImage, frame: RectF, smoothScaling: Boolean) {
    canvas().drawBitmap(image.bitmap, null, frame, if (smoothScaling) smoothImagePaint else null)
  }

  override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
    canvas().drawLine(x1, y1, x2, y2, strokePaint)
  }

  private fun canvas(): Canvas = currentPage?.canvas ?: error("draw called with no page open")
}
