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
package org.groundplatform.ui.system.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.StyleSpan
import android.util.SizeF
import androidx.core.graphics.scale
import androidx.core.graphics.withTranslation
import kotlin.math.roundToInt
import org.groundplatform.ui.model.SubmissionPdfDocument
import org.groundplatform.ui.model.SubmissionPdfDocument.Answer
import org.groundplatform.ui.model.SubmissionPdfDocument.Footer
import org.groundplatform.ui.model.SubmissionPdfDocument.Header
import org.groundplatform.ui.model.SubmissionPdfDocument.QrBlock
import org.groundplatform.ui.system.pdf.PdfConfig.BODY_SIZE
import org.groundplatform.ui.system.pdf.PdfConfig.BORDER_WIDTH
import org.groundplatform.ui.system.pdf.PdfConfig.CAPTION_SIZE
import org.groundplatform.ui.system.pdf.PdfConfig.CELL_PADDING
import org.groundplatform.ui.system.pdf.PdfConfig.FOOTER_TOP_GAP
import org.groundplatform.ui.system.pdf.PdfConfig.HEADER_BOTTOM_GAP
import org.groundplatform.ui.system.pdf.PdfConfig.HEADER_COLUMN_GAP
import org.groundplatform.ui.system.pdf.PdfConfig.IMAGE_RENDER_DPI
import org.groundplatform.ui.system.pdf.PdfConfig.LINE_SPACING
import org.groundplatform.ui.system.pdf.PdfConfig.MARGIN
import org.groundplatform.ui.system.pdf.PdfConfig.MAX_FOOTER_LINES
import org.groundplatform.ui.system.pdf.PdfConfig.MAX_HEADER_VALUE_LINES
import org.groundplatform.ui.system.pdf.PdfConfig.PAGE_HEIGHT
import org.groundplatform.ui.system.pdf.PdfConfig.PAGE_WIDTH
import org.groundplatform.ui.system.pdf.PdfConfig.PHOTO_MAX_HEIGHT
import org.groundplatform.ui.system.pdf.PdfConfig.QR_SIZE
import org.groundplatform.ui.system.pdf.PdfConfig.TITLE_SIZE
import org.groundplatform.ui.system.pdf.PdfConfig.USABLE_WIDTH
import org.groundplatform.ui.system.pdf.image.PdfImage
import org.groundplatform.ui.system.pdf.image.PdfImageSet

/**
 * Draws a [SubmissionPdfDocument] onto a [PdfDocument], one section at a time, paginating top-down.
 * Holds the mutable drawing state (current page, [Cursor], shared paints) shared by all sections.
 */
internal class PdfWriter(
  private val pdf: PdfDocument,
  private val images: PdfImageSet,
  private val totalPages: Int? = null,
) {
  private val titlePaint = textPaint(TITLE_SIZE, bold = false)
  private val bodyPaint = textPaint(BODY_SIZE, bold = false)
  private val metaTitlePaint = textPaint(CAPTION_SIZE, bold = true, textColor = Color.GRAY)
  private val metaPaint = textPaint(CAPTION_SIZE, bold = false, textColor = Color.GRAY)
  private val captionPaint = textPaint(CAPTION_SIZE, bold = false)
  private val strokePaint =
    Paint().apply {
      style = Paint.Style.STROKE
      strokeWidth = BORDER_WIDTH
      isAntiAlias = true
    }

  /** Bilinear filtering + dithering so down-scaled photos stay smooth. */
  private val photoPaint =
    Paint().apply {
      isFilterBitmap = true
      isAntiAlias = true
      isDither = true
    }

  private val cursor = Cursor()
  private var pageIndex = 0
  private var page: PdfDocument.Page? = null

  /** Top-Y of the table on the current page, or null if no table is in progress on this page. */
  private var tableTopOnPage: Float? = null

  private var header: Header? = null
  private var footerLayout: StaticLayout? = null
  private val headerLayout: HeaderLayout
    get() {
      val columnWidth = (USABLE_WIDTH - 2 * HEADER_COLUMN_GAP) / 3
      val startX = MARGIN.toFloat()

      return HeaderLayout(
        leftColumnX = startX,
        centerColumnX = startX + columnWidth + HEADER_COLUMN_GAP,
        rightColumnX = startX + 2 * (columnWidth + HEADER_COLUMN_GAP),
        columnWidth = columnWidth,
      )
    }

  val pageCount: Int
    get() = pageIndex

  fun setHeader(header: Header) {
    this.header = header
  }

  /** Pre-lays out the footer line so its height is known up front. */
  fun setFooter(footer: Footer) {
    val label = footer.dataCollectorLabel
    val text =
      SpannableString("$label: ${footer.dataCollectorName}, ${footer.userEmail}").apply {
        setSpan(StyleSpan(Typeface.BOLD), 0, label.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
      }
    val layout =
      staticLayout(text, metaPaint, PAGE_WIDTH - (2 * MARGIN), maxLines = MAX_FOOTER_LINES)
    footerLayout = layout
    cursor.footerReserve = layout.height + FOOTER_TOP_GAP
  }

  fun finalizePage() {
    flushTableDivider()
    drawFooter()
    page?.also { pdf.finishPage(it) }
    page = null
  }

  private fun canvas(): Canvas = (page ?: beginPage()).canvas

  private fun ensurePage() {
    if (page == null) beginPage()
  }

  private fun beginPage(): PdfDocument.Page {
    val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, ++pageIndex).create()
    return pdf.startPage(info).also {
      page = it
      cursor.reset()
      drawHeader()
    }
  }

  private fun newPageIfShort(spaceNeeded: Float) {
    if (!cursor.fits(spaceNeeded)) {
      finalizePage()
      beginPage()
    }
  }

  /** Survey | Job | Timestamp drawn at the top of every page. */
  private fun drawHeader() {
    val header = header ?: return
    val top = cursor.y
    val surveyLabel =
      drawText(
        header.surveyLabel,
        headerLayout.leftColumnX,
        top,
        headerLayout.columnWidth,
        metaTitlePaint,
        Layout.Alignment.ALIGN_NORMAL,
      )
    val surveyValue =
      drawText(
        header.surveyName,
        headerLayout.leftColumnX,
        surveyLabel + LINE_SPACING,
        headerLayout.columnWidth,
        metaPaint,
        Layout.Alignment.ALIGN_NORMAL,
        MAX_HEADER_VALUE_LINES,
      )

    val jobLabel =
      drawText(
        header.jobLabel,
        headerLayout.centerColumnX,
        top,
        headerLayout.columnWidth,
        metaTitlePaint,
        Layout.Alignment.ALIGN_CENTER,
      )
    val jobValue =
      drawText(
        header.jobName,
        headerLayout.centerColumnX,
        jobLabel + LINE_SPACING,
        headerLayout.columnWidth,
        metaPaint,
        Layout.Alignment.ALIGN_CENTER,
        MAX_HEADER_VALUE_LINES,
      )

    val timestampBottom =
      drawText(
        header.timestamp,
        headerLayout.rightColumnX,
        top,
        headerLayout.columnWidth,
        metaPaint,
        Layout.Alignment.ALIGN_OPPOSITE,
        MAX_HEADER_VALUE_LINES,
      )
    cursor.moveTo(maxOf(surveyValue, jobValue, timestampBottom) + HEADER_BOTTOM_GAP)
  }

  /** QR code + scan caption, right-aligned. */
  fun drawQrBlock(block: QrBlock) {
    ensurePage()
    val x = (PAGE_WIDTH - MARGIN - QR_SIZE).toFloat()
    var y = cursor.y
    images[PdfImageSet.ImageRef.Qr]?.let { qr ->
      canvas().drawImage(qr, RectF(x, y, x + QR_SIZE, y + QR_SIZE), null)
      y += QR_SIZE
    }
    y += LINE_SPACING
    y = drawText(block.scanCaption, x, y, QR_SIZE, captionPaint, Layout.Alignment.ALIGN_CENTER)
    cursor.moveTo(y + LINE_SPACING * 2)
  }

  private fun drawFooter() {
    val footer = footerLayout ?: return
    val top = PAGE_HEIGHT - MARGIN - footer.height.toFloat()
    drawStaticLayout(footer, MARGIN.toFloat(), top)
    totalPages?.let { total ->
      drawText(
        text = "$pageIndex/$total",
        x = MARGIN.toFloat(),
        y = top,
        maxWidth = USABLE_WIDTH,
        paint = metaPaint,
        alignment = Layout.Alignment.ALIGN_OPPOSITE,
        maxLines = 1,
      )
    }
  }

  fun drawTable(table: SubmissionPdfDocument.Table) {
    val rows = table.rows.takeIf { it.isNotEmpty() } ?: return
    ensurePage()
    val x = MARGIN.toFloat()
    cursor.advance(LINE_SPACING * 2)
    val label =
      SpannableString("${table.submissionLabel}: ${table.loiName}").apply {
        setSpan(
          StyleSpan(Typeface.BOLD),
          0,
          table.submissionLabel.length,
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
        )
      }
    cursor.moveTo(drawText(label, x, cursor.y, USABLE_WIDTH, titlePaint))
    cursor.advance(LINE_SPACING)
    rows.forEach { row ->
      when (val answer = row.answer) {
        is Answer.Text -> drawTableRow(row.question, answer.lines.joinToString("\n"), null)
        is Answer.Photo ->
          drawTableRow(
            row.question,
            answerText = "",
            photo = images[PdfImageSet.ImageRef.Photo(answer.remoteFilename)],
          )
      }
    }
    flushTableDivider()
  }

  private fun drawTableRow(questionText: String, answerText: String, photo: PdfImage?) {
    val questionLayout = staticLayout(questionText, bodyPaint, PdfConfig.TABLE_TASK_TEXT_WIDTH)
    val answerLayout =
      if (answerText.isEmpty()) null
      else staticLayout(answerText, bodyPaint, PdfConfig.TABLE_ANSWER_TEXT_WIDTH)
    val photoSize = photo?.let {
      val scale =
        minOf(
          PdfConfig.TABLE_ANSWER_TEXT_WIDTH.toFloat() / it.width,
          PHOTO_MAX_HEIGHT.toFloat() / it.height,
          1f,
        )
      SizeF(it.width * scale, it.height * scale)
    }
    val rowHeight = computeRowHeight(questionLayout, answerLayout, photoSize)

    newPageIfShort(rowHeight)
    val left = MARGIN.toFloat()
    if (tableTopOnPage == null) {
      tableTopOnPage = cursor.y
      canvas().drawLine(left, cursor.y, left + USABLE_WIDTH, cursor.y, strokePaint)
    }

    val top = cursor.y
    val midX = left + PdfConfig.TABLE_TASK_COLUMN_WIDTH

    drawStaticLayout(questionLayout, left + CELL_PADDING, top + CELL_PADDING)
    drawAnswerCell(midX, top, answerLayout, photo, photoSize)
    cursor.advance(rowHeight)

    canvas().drawLine(left, cursor.y, left + USABLE_WIDTH, cursor.y, strokePaint)
  }

  private fun computeRowHeight(
    questionLayout: StaticLayout,
    answerLayout: StaticLayout?,
    photoSize: SizeF?,
  ): Float {
    val answerHeight = answerLayout?.height?.toFloat() ?: 0f
    val photoHeight = photoSize?.height ?: 0f
    val photoSpacing = if (photoSize != null && answerLayout != null) LINE_SPACING else 0f
    return maxOf(questionLayout.height.toFloat(), answerHeight + photoHeight + photoSpacing) +
      2 * CELL_PADDING
  }

  /** Draws the single vertical divider for the current page's table slice and clears the marker. */
  private fun flushTableDivider() {
    val top = tableTopOnPage ?: return
    val midX = MARGIN + PdfConfig.TABLE_TASK_COLUMN_WIDTH.toFloat()
    canvas().drawLine(midX, top, midX, cursor.y, strokePaint)
    tableTopOnPage = null
  }

  private fun drawAnswerCell(
    midX: Float,
    top: Float,
    answerLayout: StaticLayout?,
    photo: PdfImage?,
    photoSize: SizeF?,
  ) {
    val cellLeft = midX + CELL_PADDING
    var y = top + CELL_PADDING
    answerLayout?.let {
      drawStaticLayout(it, cellLeft, y)
      y += it.height + LINE_SPACING
    }
    if (photo != null && photoSize != null) {
      canvas()
        .drawImage(
          photo,
          RectF(cellLeft, y, cellLeft + photoSize.width, y + photoSize.height),
          photoPaint,
        )
    }
  }

  /**
   * Lays out [text] and draws it at ([x], [y])
   *
   * @return the Y just below the drawn text.
   */
  private fun drawText(
    text: CharSequence,
    x: Float,
    y: Float,
    maxWidth: Int,
    paint: TextPaint,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    maxLines: Int = Int.MAX_VALUE,
  ): Float {
    if (text.isEmpty()) return y
    val layout = staticLayout(text, paint, maxWidth, alignment, maxLines)
    drawStaticLayout(layout, x, y)
    return y + layout.height
  }

  private fun drawStaticLayout(layout: StaticLayout, x: Float, y: Float) {
    canvas().withTranslation(x, y) { layout.draw(this) }
  }

  private fun textPaint(size: Float, bold: Boolean, textColor: Int = Color.BLACK): TextPaint =
    TextPaint().apply {
      textSize = size
      color = textColor
      isAntiAlias = true
      if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

  /**
   * Lays out [text] wrapped to [maxWidth]. When [maxLines] is set, overflow is ellipsized so a
   * single long value can't grow the layout unboundedly.
   */
  private fun staticLayout(
    text: CharSequence,
    paint: TextPaint,
    maxWidth: Int,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    maxLines: Int = Int.MAX_VALUE,
  ): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth)
      .setAlignment(alignment)
      .setLineSpacing(LINE_SPACING, 1f)
      .apply {
        if (maxLines != Int.MAX_VALUE) {
          setMaxLines(maxLines)
          setEllipsize(TextUtils.TruncateAt.END)
        }
      }
      .build()

  /**
   * Draws [image] scaled to fill [frame], scaling it down beforehand to avoid embedding
   * full-resolution bitmaps into the PDF.
   */
  private fun Canvas.drawImage(image: PdfImage, frame: RectF, paint: Paint?) {
    val bitmap = image.bitmap.downscaledFor(frame.width(), frame.height())
    drawBitmap(bitmap, null, frame, paint)
  }

  /**
   * Returns a bitmap scaled down to fit [widthPt] × [heightPt] at [IMAGE_RENDER_DPI], preserving
   * aspect ratio and never upscaling.
   *
   * Uses progressive halving (mipmap-like downsampling) for better quality and efficiency.
   * Intermediate bitmaps are recycled; the returned bitmap may be reused later by PDF rendering.
   */
  private fun Bitmap.downscaledFor(widthPt: Float, heightPt: Float): Bitmap {
    val maxWidth = PdfConfig.pointsToRenderPixels(widthPt)
    val maxHeight = PdfConfig.pointsToRenderPixels(heightPt)
    if (width <= maxWidth && height <= maxHeight) return this
    val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
    val targetWidth = (width * ratio).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * ratio).roundToInt().coerceAtLeast(1)

    var current = this
    var w = width
    var h = height
    while (w / 2 >= targetWidth && h / 2 >= targetHeight) {
      w /= 2
      h /= 2
      val halved = current.scale(w, h)
      if (current !== this) current.recycle()
      current = halved
    }
    val result = current.scale(targetWidth, targetHeight)
    if (current !== this) current.recycle()
    return result
  }

  private data class HeaderLayout(
    val leftColumnX: Float,
    val centerColumnX: Float,
    val rightColumnX: Float,
    val columnWidth: Int,
  )
}
