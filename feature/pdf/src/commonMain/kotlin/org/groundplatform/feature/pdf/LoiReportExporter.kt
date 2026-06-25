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
package org.groundplatform.feature.pdf

import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.feature.pdf.mapper.LoiReportMapper
import org.groundplatform.ui.components.loireport.LoiReportAction

class LoiReportExporter(
  private val mapper: LoiReportMapper,
  private val exportService: PdfExportService,
) {
  suspend fun export(loiReport: LoiReport, action: LoiReportAction): Result<Unit> = runCatching {
    val pdfAction =
      when (action) {
        LoiReportAction.OnShareClicked -> PdfExportService.Action.Share
        LoiReportAction.OnPdfItemClicked -> PdfExportService.Action.Open
      }

    val submission =
      loiReport.submissionDetails?.submissions?.firstOrNull() ?: error("No submission to export")
    val request = mapper.map(loiReport, submission) ?: error("Failed to map LoiReport")
    exportService.export(request, pdfAction)
  }
}
