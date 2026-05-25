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
package org.groundplatform.ui.mapper

import ground_android.core.ui.generated.resources.Res
import ground_android.core.ui.generated.resources.job
import ground_android.core.ui.generated.resources.pdf_details_data_collector_label
import ground_android.core.ui.generated.resources.scan_this_qr_to_download_geojson
import ground_android.core.ui.generated.resources.submission
import ground_android.core.ui.generated.resources.survey
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.domain.model.submission.Submission
import org.groundplatform.domain.model.task.PhotoTaskData
import org.groundplatform.ui.model.SubmissionPdfDocument
import org.groundplatform.ui.model.SubmissionPdfDocument.Answer
import org.groundplatform.ui.model.SubmissionPdfDocument.Footer
import org.groundplatform.ui.model.SubmissionPdfDocument.Header
import org.groundplatform.ui.model.SubmissionPdfDocument.QrBlock
import org.groundplatform.ui.model.SubmissionPdfDocument.Row
import org.groundplatform.ui.system.pdf.PdfExportService
import org.groundplatform.ui.util.formatDate
import org.groundplatform.ui.util.formatTime
import org.jetbrains.compose.resources.getString

object LoiReportMapper {

  suspend fun map(loiReport: LoiReport, submission: Submission): PdfExportService.Request? {
    val details = loiReport.submissionDetails ?: return null
    val rows = buildRows(submission)
    val document =
      SubmissionPdfDocument(
        header = buildHeader(details, submission),
        qrBlock = buildQrBlock(),
        footer = buildFooter(details),
        table =
          SubmissionPdfDocument.Table(
            submissionLabel = getString(Res.string.submission),
            loiName = loiReport.loiName,
            rows = rows,
          ),
      )
    val fileName =
      listOf(loiReport.loiName, details.userName, details.dateMillis.toString())
        .joinToString("_") { it.filter(::isSafeFileChar) }
        .trim('_')

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
      surveyLabel = getString(Res.string.survey),
      surveyName = details.surveyName,
      jobLabel = getString(Res.string.job),
      jobName = submission.job.name ?: submission.job.id,
      timestamp = "${formatDate(details.dateMillis)} ${formatTime(details.dateMillis)}",
    )

  private suspend fun buildQrBlock(): QrBlock =
    QrBlock(scanCaption = getString(Res.string.scan_this_qr_to_download_geojson))

  private suspend fun buildFooter(details: LoiReport.SubmissionDetails): Footer =
    Footer(
      dataCollectorLabel = getString(Res.string.pdf_details_data_collector_label),
      dataCollectorName = details.userName,
      userEmail = details.userEmail,
    )

  private suspend fun buildRows(submission: Submission): List<Row> =
    submission.job.tasksSorted.mapNotNull { task ->
      if (task.isOmittedFromDocExport()) return@mapNotNull null

      val value = submission.data.getValue(task.id) ?: return@mapNotNull null

      Row(
        question = task.label,
        answer =
          when (value) {
            is PhotoTaskData -> Answer.Photo(value.remoteFilename)
            else -> Answer.Text(TaskValueMapper.map(task, value).split("\n"))
          },
      )
    }

  private fun isSafeFileChar(c: Char): Boolean = c.isLetterOrDigit() || c in "_-"
}
