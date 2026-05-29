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
package org.groundplatform.feature.pdf.mapper

import ground_android.core.ui.generated.resources.Res
import ground_android.core.ui.generated.resources.job
import ground_android.core.ui.generated.resources.pdf_data_collector
import ground_android.core.ui.generated.resources.scan_this_qr_to_download_geojson
import ground_android.core.ui.generated.resources.submission
import ground_android.core.ui.generated.resources.survey
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.domain.model.submission.Submission
import org.groundplatform.feature.pdf.PdfExportService
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument.Footer
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument.Header
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument.QrBlock
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument.Row
import org.groundplatform.feature.pdf.model.SubmissionPdfDocument.Table
import org.groundplatform.ui.util.DateFormatter
import org.groundplatform.ui.util.StringResolver

/** Maps an [LoiReport] and its [Submission] to a [PdfExportService.Request]. */
class LoiReportMapper(
  private val taskValueMapper: TaskValueMapper,
  private val strings: StringResolver,
  private val dateFormatter: DateFormatter,
) {

  suspend fun map(loiReport: LoiReport, submission: Submission): PdfExportService.Request? {
    val details = loiReport.submissionDetails ?: return null
    val rows = buildRows(submission)
    val document =
      SubmissionPdfDocument(
        header = buildHeader(details, submission),
        qrBlock = buildQrBlock(),
        footer = buildFooter(details),
        table =
          Table(
            submissionLabel = strings.resolve(Res.string.submission),
            loiName = loiReport.loiName,
            rows = rows,
          ),
      )
    val timestamp =
      "${dateFormatter.formatDate(details.dateMillis)}_${dateFormatter.formatTime(details.dateMillis)}"
    val fileName =
      listOf(details.surveyName, loiReport.loiName, details.userName, timestamp)
        .map { it.filter(::isSafeFileChar) }
        .filter { it.isNotBlank() }
        .joinToString("_")
        .take(200)

    return PdfExportService.Request(
      document = document,
      qrContent = loiReport.geoJson.toString(),
      fileName = fileName,
    )
  }

  private suspend fun buildHeader(
    details: LoiReport.SubmissionDetails,
    submission: Submission,
  ): Header =
    Header(
      surveyLabel = strings.resolve(Res.string.survey),
      surveyName = details.surveyName,
      jobLabel = strings.resolve(Res.string.job),
      jobName = submission.job.name ?: submission.job.id,
      timestamp =
        "${dateFormatter.formatDate(details.dateMillis)} ${dateFormatter.formatTime(details.dateMillis)}",
    )

  private suspend fun buildQrBlock(): QrBlock =
    QrBlock(scanCaption = strings.resolve(Res.string.scan_this_qr_to_download_geojson))

  private suspend fun buildFooter(details: LoiReport.SubmissionDetails): Footer =
    Footer(
      dataCollectorLabel = strings.resolve(Res.string.pdf_data_collector),
      dataCollectorName = details.userName,
      userEmail = details.userEmail,
    )

  private suspend fun buildRows(submission: Submission): List<Row> =
    submission.job.tasksSorted
      .filter { !it.isOmittedFromDocExport() }
      .mapNotNull { task ->
        submission.data.getValue(task.id)?.let { value ->
          Row(question = task.label, answer = taskValueMapper.map(task, value))
        }
      }

  private fun isSafeFileChar(c: Char): Boolean =
    c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in "_-"
}
