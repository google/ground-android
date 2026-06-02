package org.groundplatform.feature.pdf.render

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import org.groundplatform.feature.pdf.render.PdfConfig.BODY_SIZE
import org.groundplatform.feature.pdf.render.PdfConfig.CAPTION_SIZE
import org.groundplatform.feature.pdf.render.PdfConfig.TITLE_SIZE

internal class PdfTextPaints {
  val title: TextPaint = textPaint(TITLE_SIZE, bold = false)
  val body: TextPaint = textPaint(BODY_SIZE, bold = false)
  val metaLabel: TextPaint = textPaint(CAPTION_SIZE, bold = true, textColor = Color.GRAY)
  val meta: TextPaint = textPaint(CAPTION_SIZE, bold = false, textColor = Color.GRAY)
  val caption: TextPaint = textPaint(CAPTION_SIZE, bold = false)

  private fun textPaint(size: Float, bold: Boolean, textColor: Int = Color.BLACK): TextPaint =
    TextPaint().apply {
      textSize = size
      color = textColor
      isAntiAlias = true
      if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
}
