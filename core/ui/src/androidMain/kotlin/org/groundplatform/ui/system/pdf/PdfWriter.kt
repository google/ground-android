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
import org.groundplatform.ui.system.pdf.PdfConfig.FOOTER_TEXT_MAX_WIDTH
import org.groundplatform.ui.system.pdf.PdfConfig.FOOTER_TOP_GAP
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
import org.groundplatform.ui.system.pdf.components.PageFooterLayout
import org.groundplatform.ui.system.pdf.components.PageHeaderLayout
import org.groundplatform.ui.system.pdf.components.QrBlockLayout
import org.groundplatform.ui.system.pdf.components.TableRowLayout
import org.groundplatform.ui.system.pdf.image.PdfImage
import org.groundplatform.ui.system.pdf.image.PdfImageSet

/**
 * Draws a [SubmissionPdfDocument] onto a [PdfDocument], one section at a time, paginating top-down.
 * Holds the mutable drawing state (current page, [PdfCursor], shared paints) shared by all
 * sections.
 */
internal class PdfWriter(
  private val pdf: PdfDocument,
  private val images: PdfImageSet,
  private val totalPages: Int? = null,
  private val header: Header,
  footer: Footer,
) : PdfPageController.PageLifecycle {
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

  private val scaledImagePaint =
    Paint().apply {
      isFilterBitmap = true
      isAntiAlias = true
      isDither = true
    }

  private val cursor = PdfCursor()
  private val pageController = PdfPageController(cursor, this)
  private var currentPage: PdfDocument.Page? = null

  private var currentTableTopY: Float? = null

  private val footerLayout: StaticLayout

  init {
    val footerLabel = footer.dataCollectorLabel
    val footerText =
      SpannableString("$footerLabel: ${footer.dataCollectorName}, ${footer.userEmail}").apply {
        setSpan(StyleSpan(Typeface.BOLD), 0, footerLabel.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
      }
    footerLayout =
      staticLayout(footerText, metaPaint, FOOTER_TEXT_MAX_WIDTH, maxLines = MAX_FOOTER_LINES)
    cursor.footerReserve = footerLayout.height + FOOTER_TOP_GAP
  }

  val pageCount: Int
    get() = pageController.pageCount

  override fun onPageStarted(pageNumber: Int) {
    val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
    currentPage = pdf.startPage(info)
    drawPageHeader()
  }

  override fun onPageEnding(pageNumber: Int) {
    flushTableDivider()
    drawPageFooter()
    currentPage?.also { pdf.finishPage(it) }
    currentPage = null
  }

  fun drawQrBlock(block: QrBlock) {
    pageController.ensurePage()
    val qr = images[PdfImageSet.ImageRef.Qr]
    val captionLayout =
      staticLayout(block.scanCaption, captionPaint, QR_SIZE, Layout.Alignment.ALIGN_CENTER)
    val layout =
      QrBlockLayout.compute(
        top = cursor.y,
        hasQrImage = qr != null,
        captionHeight = captionLayout.height.toFloat(),
      )
    if (qr != null && layout.qrFrame != null) {
      drawImage(qr, layout.qrFrame, paint = null)
    }
    drawStaticLayoutAt(captionLayout, layout.captionOffset)
    cursor.moveTo(layout.nextCursorY)
  }

  fun drawTable(table: SubmissionPdfDocument.Table) {
    val rows = table.rows.takeIf { it.isNotEmpty() } ?: return
    pageController.ensurePage()
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
        is Answer.Text ->
          drawTableRow(
            questionText = row.question,
            answerText = answer.lines.joinToString("\n"),
            photo = null,
          )
        is Answer.Photo ->
          drawTableRow(
            questionText = row.question,
            answerText = "",
            photo = images[PdfImageSet.ImageRef.Photo(answer.remoteFilename)],
          )
      }
    }
    flushTableDivider()
  }

  fun finalizePage() {
    pageController.finalizePage()
  }

  private fun canvas(): Canvas = currentPage?.canvas ?: error("canvas() called with no page open")

  private fun drawPageHeader() {
    val columnWidth = PageHeaderLayout.COLUMN_WIDTH
    val surveyLabel = staticLayout(header.surveyLabel, metaTitlePaint, columnWidth)
    val surveyValue =
      staticLayout(header.surveyName, metaPaint, columnWidth, maxLines = MAX_HEADER_VALUE_LINES)
    val jobLabel =
      staticLayout(header.jobLabel, metaTitlePaint, columnWidth, Layout.Alignment.ALIGN_CENTER)
    val jobValue =
      staticLayout(
        header.jobName,
        metaPaint,
        columnWidth,
        alignment = Layout.Alignment.ALIGN_CENTER,
        maxLines = MAX_HEADER_VALUE_LINES,
      )
    val timestamp =
      staticLayout(
        header.timestamp,
        metaPaint,
        columnWidth,
        alignment = Layout.Alignment.ALIGN_OPPOSITE,
        maxLines = MAX_HEADER_VALUE_LINES,
      )

    val layout =
      PageHeaderLayout.compute(
        top = cursor.y,
        labelHeight = surveyLabel.height.toFloat(),
        valueHeight = surveyValue.height.toFloat(),
      )

    drawStaticLayoutAt(surveyLabel, layout.leftColumn.labelOffset)
    drawStaticLayoutAt(surveyValue, layout.leftColumn.valueOffset)
    drawStaticLayoutAt(jobLabel, layout.centerColumn.labelOffset)
    drawStaticLayoutAt(jobValue, layout.centerColumn.valueOffset)
    drawStaticLayoutAt(timestamp, layout.rightTextOffset)
    cursor.moveTo(layout.nextCursorY)
  }

  private fun drawPageFooter() {
    val layout = PageFooterLayout.compute(footerHeight = footerLayout.height.toFloat())
    drawStaticLayoutAt(footerLayout, layout.footerOffset)
    totalPages?.let { total ->
      drawText(
        text = "${pageController.pageCount}/$total",
        x = layout.pageNumberOffset.x,
        y = layout.pageNumberOffset.y,
        maxWidth = layout.pageNumberMaxWidth,
        paint = metaPaint,
        alignment = Layout.Alignment.ALIGN_OPPOSITE,
        maxLines = 1,
      )
    }
  }

  private fun drawTableRow(questionText: String, answerText: String, photo: PdfImage?) {
    val questionLayout = staticLayout(questionText, bodyPaint, PdfConfig.TABLE_TASK_TEXT_WIDTH)
    val answerLayout =
      if (answerText.isEmpty()) null
      else staticLayout(answerText, bodyPaint, PdfConfig.TABLE_ANSWER_TEXT_WIDTH)
    val photoSize = photo?.let {
      PdfRenderer.fitInside(
        it.width,
        it.height,
        PdfConfig.TABLE_ANSWER_TEXT_WIDTH,
        PHOTO_MAX_HEIGHT,
      )
    }

    val questionHeight = questionLayout.height.toFloat()
    val answerHeight = answerLayout?.height?.toFloat() ?: 0f
    pageController.newPageIfShort(
      TableRowLayout.totalHeight(questionHeight, answerHeight, photoSize)
    )
    val rowLayout =
      TableRowLayout.compute(
        rowTop = cursor.y,
        leftTextHeight = questionHeight,
        rightTextHeight = answerHeight,
        rightImageSize = photoSize,
      )

    if (currentTableTopY == null) {
      currentTableTopY = cursor.y
      canvas().drawLine(rowLayout.rowLeft, cursor.y, rowLayout.rowRight, cursor.y, strokePaint)
    }

    drawStaticLayoutAt(questionLayout, rowLayout.leftTextOffset)
    if (answerLayout != null && rowLayout.rightTextOffset != null) {
      drawStaticLayoutAt(answerLayout, rowLayout.rightTextOffset)
    }
    if (photo != null && rowLayout.rightImageFrame != null) {
      drawImage(photo, rowLayout.rightImageFrame, scaledImagePaint)
    }
    cursor.advance(rowLayout.totalHeight)

    canvas().drawLine(rowLayout.rowLeft, cursor.y, rowLayout.rowRight, cursor.y, strokePaint)
  }

  private fun flushTableDivider() {
    val top = currentTableTopY ?: return
    val midX = MARGIN + PdfConfig.TABLE_TASK_COLUMN_WIDTH.toFloat()
    canvas().drawLine(midX, top, midX, cursor.y, strokePaint)
    currentTableTopY = null
  }

  /**
   * Lays out [text] and draws it at ([x], [y]).
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

  private fun drawStaticLayoutAt(layout: StaticLayout, offset: PdfRenderer.Offset) =
    drawStaticLayout(layout, offset.x, offset.y)

  private fun drawImage(image: PdfImage, frame: PdfRenderer.Rect, paint: Paint?) {
    canvas().drawImage(image, RectF(frame.x, frame.y, frame.right, frame.bottom), paint)
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
   * Returns a bitmap scaled down to fit [widthPt] × [heightPt], preserving aspect ratio and never
   * upscaling. Uses progressive halving for efficiency; intermediate bitmaps are recycled.
   */
  private fun Bitmap.downscaledFor(widthPt: Float, heightPt: Float): Bitmap {
    val maxWidth = PdfRenderer.pointsToRenderPixels(widthPt)
    val maxHeight = PdfRenderer.pointsToRenderPixels(heightPt)
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
}
