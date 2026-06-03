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
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.StyleSpan
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument.Answer
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument.Footer
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument.Header
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument.QrBlock
import org.groundplatform.feature.pdf.render.PdfConfig.FOOTER_TEXT_MAX_WIDTH
import org.groundplatform.feature.pdf.render.PdfConfig.LINE_SPACING
import org.groundplatform.feature.pdf.render.PdfConfig.MAX_FOOTER_LINES
import org.groundplatform.feature.pdf.render.PdfConfig.MAX_HEADER_VALUE_LINES
import org.groundplatform.feature.pdf.render.PdfConfig.PHOTO_MAX_HEIGHT
import org.groundplatform.feature.pdf.render.PdfConfig.QR_SIZE
import org.groundplatform.feature.pdf.render.PdfConfig.USABLE_WIDTH
import org.groundplatform.feature.pdf.render.components.PageFooterLayout
import org.groundplatform.feature.pdf.render.components.PageHeaderLayout
import org.groundplatform.feature.pdf.render.components.QrBlockLayout
import org.groundplatform.feature.pdf.render.components.TableLayout
import org.groundplatform.feature.pdf.render.image.PdfImage
import org.groundplatform.feature.pdf.render.image.PdfImageSet

/**
 * Draws a [SubmissionPdfDocument] onto a [PdfDocument], one section at a time, paginating top-down.
 * Holds the mutable drawing state (current page, [PdfCursor], shared paints) shared by all
 * sections.
 */
internal class PdfWriter(
  private val pdfCanvas: PdfCanvas,
  private val images: PdfImageSet,
  private val totalPages: Int? = null,
  private val header: Header,
  footer: Footer,
) : PdfPageController.PageLifecycle {
  private val paints = PdfTextPaints()

  private val footerLayout: StaticLayout = buildFooterLayout(footer)
  private val cursor =
    PdfCursor(footerReserve = PageFooterLayout.reserve(footerLayout.height.toFloat()))
  private val pageController = PdfPageController(cursor, this)

  val pageCount: Int
    get() = pageController.pageCount

  override fun onPageStarted(pageNumber: Int) {
    pdfCanvas.startPage(pageNumber)
    drawPageHeader()
  }

  override fun onPageEnding(pageNumber: Int) {
    drawPageFooter()
    pdfCanvas.finishPage()
  }

  fun drawQrBlock(block: QrBlock) {
    val qr = images[PdfImageSet.ImageRef.Qr] ?: return
    pageController.ensurePage()
    val captionLayout =
      staticLayout(block.scanCaption, paints.caption, QR_SIZE, Layout.Alignment.ALIGN_CENTER)
    val layout =
      QrBlockLayout.compute(top = cursor.y, captionHeight = captionLayout.height.toFloat())
    drawImage(qr, layout.qrFrame, smoothScaling = false)
    drawStaticLayoutAt(captionLayout, layout.captionOffset)
    cursor.moveTo(layout.nextCursorY)
  }

  fun drawTable(table: SubmissionPdfDocument.Table) {
    val rows = table.rows.takeIf { it.isNotEmpty() } ?: return
    pageController.ensurePage()
    val label =
      SpannableString("${table.submissionLabel}: ${table.loiName}").apply {
        setSpan(
          StyleSpan(Typeface.BOLD),
          0,
          table.submissionLabel.length,
          Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
        )
      }
    val labelLayout = staticLayout(label, paints.title, USABLE_WIDTH)
    val tableLabel = TableLayout.getLabel(top = cursor.y, labelHeight = labelLayout.height.toFloat())
    drawStaticLayoutAt(labelLayout, tableLabel.labelOffset)
    cursor.moveTo(tableLabel.nextCursorY)
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
  }

  fun finalizePage() {
    pageController.finalizePage()
  }

  private fun drawPageHeader() {
    val columnWidth = PageHeaderLayout.COLUMN_WIDTH
    val surveyLabel = staticLayout(header.surveyLabel, paints.metaLabel, columnWidth)
    val surveyValue =
      staticLayout(header.surveyName, paints.meta, columnWidth, maxLines = MAX_HEADER_VALUE_LINES)
    val jobLabel =
      staticLayout(header.jobLabel, paints.metaLabel, columnWidth, Layout.Alignment.ALIGN_CENTER)
    val jobValue =
      staticLayout(
        header.jobName,
        paints.meta,
        columnWidth,
        alignment = Layout.Alignment.ALIGN_CENTER,
        maxLines = MAX_HEADER_VALUE_LINES,
      )
    val timestamp =
      staticLayout(
        header.timestamp,
        paints.meta,
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
    drawStaticLayoutAt(footerLayout, layout.footerTextOffset)
    totalPages?.let { total ->
      val pageNumber =
        staticLayout(
          "${pageController.pageCount}/$total",
          paints.meta,
          layout.pageNumberMaxWidth,
          alignment = Layout.Alignment.ALIGN_OPPOSITE,
          maxLines = 1,
        )
      drawStaticLayoutAt(pageNumber, layout.pageNumberOffset)
    }
  }

  private fun drawTableRow(questionText: String, answerText: String, photo: PdfImage?) {
    val questionLayout = staticLayout(questionText, paints.body, PdfConfig.TABLE_TASK_TEXT_WIDTH)
    val answerLayout =
      if (answerText.isEmpty()) null
      else staticLayout(answerText, paints.body, PdfConfig.TABLE_ANSWER_TEXT_WIDTH)
    val photoSize = photo?.let {
      fitInside(it.width, it.height, PdfConfig.TABLE_ANSWER_TEXT_WIDTH, PHOTO_MAX_HEIGHT)
    }

    val questionHeight = questionLayout.height.toFloat()
    val answerHeight = answerLayout?.height?.toFloat() ?: 0f
    pageController.newPageIfShort(TableLayout.getRowHeight(questionHeight, answerHeight, photoSize))
    val rowLayout =
      TableLayout.getRow(
        rowTop = cursor.y,
        leftTextHeight = questionHeight,
        rightTextHeight = answerHeight,
        rightImageSize = photoSize,
      )

    rowLayout.borderLines.forEach { drawLine(it) }
    drawStaticLayoutAt(questionLayout, rowLayout.leftTextOffset)
    if (answerLayout != null && rowLayout.rightTextOffset != null) {
      drawStaticLayoutAt(answerLayout, rowLayout.rightTextOffset)
    }
    if (photo != null && rowLayout.rightImageFrame != null) {
      drawImage(photo, rowLayout.rightImageFrame, smoothScaling = true)
    }
    cursor.advance(rowLayout.totalHeight)
  }

  private fun drawStaticLayoutAt(layout: StaticLayout, offset: PdfOffset) =
    pdfCanvas.drawStaticLayout(layout, offset.x, offset.y)

  private fun drawImage(image: PdfImage, frame: PdfRect, smoothScaling: Boolean) =
    pdfCanvas.drawImage(image, RectF(frame.x, frame.y, frame.right, frame.bottom), smoothScaling)

  private fun drawLine(line: PdfLine) =
    pdfCanvas.drawLine(line.startX, line.startY, line.endX, line.endY)

  private fun buildFooterLayout(footer: Footer): StaticLayout {
    val footerLabel = footer.dataCollectorLabel
    val footerText =
      SpannableString("$footerLabel: ${footer.dataCollectorName}, ${footer.userEmail}").apply {
        setSpan(StyleSpan(Typeface.BOLD), 0, footerLabel.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
      }
    return staticLayout(footerText, paints.meta, FOOTER_TEXT_MAX_WIDTH, maxLines = MAX_FOOTER_LINES)
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
}
