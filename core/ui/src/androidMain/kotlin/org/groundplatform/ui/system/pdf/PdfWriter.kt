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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.SizeF
import androidx.core.graphics.withTranslation
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
import org.groundplatform.ui.system.pdf.PdfConfig.HEADER_SIZE
import org.groundplatform.ui.system.pdf.PdfConfig.LINE_SPACING
import org.groundplatform.ui.system.pdf.PdfConfig.MARGIN
import org.groundplatform.ui.system.pdf.PdfConfig.MAX_FOOTER_LINES
import org.groundplatform.ui.system.pdf.PdfConfig.MAX_HEADER_VALUE_LINES
import org.groundplatform.ui.system.pdf.PdfConfig.PAGE_HEIGHT
import org.groundplatform.ui.system.pdf.PdfConfig.PAGE_WIDTH
import org.groundplatform.ui.system.pdf.PdfConfig.PHOTO_MAX_HEIGHT
import org.groundplatform.ui.system.pdf.PdfConfig.QR_SIZE
import org.groundplatform.ui.system.pdf.PdfConfig.TABLE_QUESTION_RATIO
import org.groundplatform.ui.system.pdf.image.PdfImage
import org.groundplatform.ui.system.pdf.image.PdfImageSet

/**
 * Draws a [SubmissionPdfDocument] onto a [PdfDocument], one section at a time, paginating top-down.
 * Holds the mutable drawing state (current page, [Cursor], shared paints) shared by all sections.
 */
internal class PdfWriter(private val pdf: PdfDocument, private val images: PdfImageSet) {
  private val headerPaint = textPaint(HEADER_SIZE, bold = true)
  private val bodyPaint = textPaint(BODY_SIZE, bold = false)
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
  private var footerLayouts: FooterLayouts? = null

  fun setHeader(header: Header) {
    this.header = header
  }

  /** Pre-lays out the footer columns so its height is known up front. */
  fun setFooter(footer: Footer) {
    val cols = threeColumnLayout()
    footerLayouts =
      FooterLayouts(
        label = staticLayout(footer.dataCollectorLabel, headerPaint, cols.colWidth, maxLines = 1),
        name =
          staticLayout(
            footer.dataCollectorName,
            bodyPaint,
            cols.colWidth,
            Layout.Alignment.ALIGN_CENTER,
            MAX_FOOTER_LINES,
          ),
        email =
          staticLayout(
            footer.userEmail,
            bodyPaint,
            cols.colWidth,
            Layout.Alignment.ALIGN_OPPOSITE,
            MAX_FOOTER_LINES,
          ),
        leftX = cols.leftX,
        centerX = cols.centerX,
        rightX = cols.rightX,
      )
    cursor.footerReserve = footerLayouts!!.height + FOOTER_TOP_GAP
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
    val cols = threeColumnLayout()
    val top = cursor.y
    val surveyBottom =
      drawLabeledColumn(header.surveyLabel, header.surveyName, cols.leftX, cols.colWidth, top)
    val jobBottom =
      drawLabeledColumn(
        header.jobLabel,
        header.jobName,
        cols.centerX,
        cols.colWidth,
        top,
        Layout.Alignment.ALIGN_CENTER,
      )
    val timestampBottom =
      drawText(
        header.timestamp,
        cols.rightX,
        top,
        cols.colWidth,
        bodyPaint,
        Layout.Alignment.ALIGN_OPPOSITE,
        MAX_HEADER_VALUE_LINES,
      )
    cursor.moveTo(maxOf(surveyBottom, jobBottom, timestampBottom) + HEADER_BOTTOM_GAP)
  }

  /** Draws a bold [label] above its [value] in one header column; returns the column's bottom Y. */
  private fun drawLabeledColumn(
    label: String,
    value: String,
    x: Float,
    width: Int,
    top: Float,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
  ): Float {
    val afterLabel = drawText(label, x, top, width, headerPaint, alignment)
    return drawText(
      value,
      x,
      afterLabel + LINE_SPACING,
      width,
      bodyPaint,
      alignment,
      MAX_HEADER_VALUE_LINES,
    )
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

  /** Data Collector | Name | Email anchored to the bottom of every page. */
  private fun drawFooter() {
    val footer = footerLayouts ?: return
    val top = PAGE_HEIGHT - MARGIN - footer.height
    drawStaticLayout(footer.label, footer.leftX, top)
    drawStaticLayout(footer.name, footer.centerX, top)
    drawStaticLayout(footer.email, footer.rightX, top)
  }

  /** Pre-laid-out footer columns and their X positions, sized once in [setFooter]. */
  private class FooterLayouts(
    val label: StaticLayout,
    val name: StaticLayout,
    val email: StaticLayout,
    val leftX: Float,
    val centerX: Float,
    val rightX: Float,
  ) {
    val height: Float = maxOf(label.height, name.height, email.height).toFloat()
  }

  private fun threeColumnLayout(): ThreeColumns {
    val usable = PAGE_WIDTH - 2 * MARGIN
    val colWidth = (usable - 2 * HEADER_COLUMN_GAP) / 3
    val leftX = MARGIN.toFloat()
    val centerX = leftX + colWidth + HEADER_COLUMN_GAP
    val rightX = centerX + colWidth + HEADER_COLUMN_GAP
    return ThreeColumns(leftX, centerX, rightX, colWidth)
  }

  private data class ThreeColumns(
    val leftX: Float,
    val centerX: Float,
    val rightX: Float,
    val colWidth: Int,
  )

  fun drawTable(table: SubmissionPdfDocument.Table) {
    val rows = table.rows.takeIf { it.isNotEmpty() } ?: return
    ensurePage()
    val width = PAGE_WIDTH - 2 * MARGIN
    val x = MARGIN.toFloat()
    cursor.advance(LINE_SPACING * 2)
    val label = "${table.submissionLabel}: ${table.loiName}"
    cursor.moveTo(drawText(label, x, cursor.y, width, headerPaint))
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
    val questionLayout = staticLayout(questionText, bodyPaint, TABLE.questionTextWidth)
    val answerLayout =
      if (answerText.isEmpty()) null else staticLayout(answerText, bodyPaint, TABLE.answerTextWidth)
    val photoSize = photo?.let { fitInside(it, TABLE.answerTextWidth, PHOTO_MAX_HEIGHT) }
    val rowHeight = computeRowHeight(questionLayout, answerLayout, photoSize)

    newPageIfShort(rowHeight)
    val left = MARGIN.toFloat()
    if (tableTopOnPage == null) {
      tableTopOnPage = cursor.y
      canvas().drawLine(left, cursor.y, left + TABLE.width, cursor.y, strokePaint)
    }

    val top = cursor.y
    val midX = left + TABLE.questionColWidth

    drawStaticLayout(questionLayout, left + CELL_PADDING, top + CELL_PADDING)
    drawAnswerCell(midX, top, answerLayout, photo, photoSize)
    cursor.advance(rowHeight)

    canvas().drawLine(left, cursor.y, left + TABLE.width, cursor.y, strokePaint)
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
    val midX = MARGIN + TABLE.questionColWidth.toFloat()
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
      // Centre horizontally within the answer cell.
      val photoX = cellLeft + (TABLE.answerTextWidth - photoSize.width) / 2f
      canvas()
        .drawImage(
          photo,
          RectF(photoX, y, photoX + photoSize.width, y + photoSize.height),
          photoPaint,
        )
    }
  }

  /** Lays out [text] and draws it at ([x], [y]); returns the Y just below the drawn text. */
  private fun drawText(
    text: String,
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

  /** A text paint at [size] points, [bold] for labels. */
  private fun textPaint(size: Float, bold: Boolean): TextPaint =
    TextPaint().apply {
      textSize = size
      isAntiAlias = true
      if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

  /**
   * Lays out [text] wrapped to [maxWidth]. When [maxLines] is set, overflow is ellipsized so a
   * single long value can't grow the layout unboundedly.
   */
  private fun staticLayout(
    text: String,
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
   * Scales [image] to fit within [maxWidth] x [maxHeight], keeping aspect ratio, never upscaling.
   */
  private fun fitInside(image: PdfImage, maxWidth: Int, maxHeight: Int): SizeF {
    val scale = minOf(maxWidth.toFloat() / image.width, maxHeight.toFloat() / image.height, 1f)
    return SizeF(image.width * scale, image.height * scale)
  }

  /** Draws [image] scaled to fill [dst]. */
  private fun Canvas.drawImage(image: PdfImage, dst: RectF, paint: Paint?) {
    drawBitmap(image.bitmap, Rect(0, 0, image.bitmap.width, image.bitmap.height), dst, paint)
  }
}

/** Column widths for the question/answer table. */
private val TABLE = run {
  val tableWidth = PAGE_WIDTH - 2 * MARGIN
  val questionColWidth = (tableWidth * TABLE_QUESTION_RATIO).toInt()
  object {
    val width = tableWidth
    val questionColWidth = questionColWidth
    val questionTextWidth = questionColWidth - 2 * CELL_PADDING
    val answerTextWidth = tableWidth - questionColWidth - 2 * CELL_PADDING
  }
}
