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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.groundplatform.feature.pdf.helpers.FakeDateFormatter
import org.groundplatform.feature.pdf.helpers.FakePdfExportService
import org.groundplatform.feature.pdf.helpers.FakeStringResolver
import org.groundplatform.feature.pdf.mapper.LoiReportMapper
import org.groundplatform.feature.pdf.mapper.TaskValueMapper
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.ui.components.loireport.LoiReportAction

class LoiReportExporterTest {

  private val mapper =
    LoiReportMapper(
      taskValueMapper =
        TaskValueMapper(strings = FakeStringResolver, dateFormatter = FakeDateFormatter),
      strings = FakeStringResolver,
      dateFormatter = FakeDateFormatter,
    )

  @Test
  fun `opens the report when the action is OnPdfItemClicked`() = runTest {
    val service = FakePdfExportService()
    val exporter = LoiReportExporter(mapper, service.service)

    val result = exporter.export(FakeDataGenerator.newLoiReport(), LoiReportAction.OnPdfItemClicked)

    assertTrue(result.isSuccess)
    assertEquals(service.outputPath, service.openedPath)
    assertNull(service.sharedPath)
  }

  @Test
  fun `shares the report when the action is OnShareClicked`() = runTest {
    val service = FakePdfExportService()
    val exporter = LoiReportExporter(mapper, service.service)

    val result = exporter.export(FakeDataGenerator.newLoiReport(), LoiReportAction.OnShareClicked)

    assertTrue(result.isSuccess)
    assertEquals(service.outputPath, service.sharedPath)
    assertNull(service.openedPath)
  }

  @Test
  fun `returns failure without exporting when report has no submission`() = runTest {
    val service = FakePdfExportService()
    val exporter = LoiReportExporter(mapper, service.service)
    val loiReport =
      FakeDataGenerator.newLoiReport(
        submissionDetails = FakeDataGenerator.newSubmissionDetails(submissions = emptyList())
      )

    val result = exporter.export(loiReport, LoiReportAction.OnPdfItemClicked)

    assertTrue(result.isFailure)
    assertNull(service.openedPath)
  }

  @Test
  fun `returns failure when export throws`() = runTest {
    val service = FakePdfExportService().apply { renderError = RuntimeException("boom") }
    val exporter = LoiReportExporter(mapper, service.service)

    val result = exporter.export(FakeDataGenerator.newLoiReport(), LoiReportAction.OnPdfItemClicked)

    assertTrue(result.isFailure)
  }
}
