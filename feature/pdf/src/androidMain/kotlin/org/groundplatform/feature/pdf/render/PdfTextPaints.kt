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
