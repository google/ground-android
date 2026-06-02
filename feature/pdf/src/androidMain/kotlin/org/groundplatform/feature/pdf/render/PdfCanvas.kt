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
}

/** Used during the page-counting phase. Drops every drawing call. */
internal object MeasurementPdfCanvas : PdfCanvas {
  override fun startPage(pageNumber: Int) = Unit

  override fun finishPage() = Unit

  override fun drawStaticLayout(layout: StaticLayout, x: Float, y: Float) = Unit

  override fun drawImage(image: PdfImage, frame: RectF, smoothScaling: Boolean) = Unit

  override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) = Unit
}