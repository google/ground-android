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
package org.groundplatform.feature.pdf.model

/**
 * UI model for a submission PDF. Each property corresponds to a distinct visual section so that
 * platform renderers (Android, iOS) can lay them out independently:
 */
data class SubmissionPdfDocument(
  val header: Header,
  val qrBlock: QrBlock,
  val footer: Footer,
  val table: Table,
) {

  data class Header(
    val surveyLabel: String,
    val surveyName: String,
    val jobLabel: String,
    val jobName: String,
    val timestamp: String,
  )

  data class QrBlock(val scanCaption: String)

  data class Table(val submissionLabel: String, val loiName: String, val rows: List<Row>)

  data class Row(val question: String, val answer: Answer)

  sealed interface Answer {
    data class Text(val lines: List<String>) : Answer

    data class Photo(val remoteFilename: String) : Answer
  }

  data class Footer(
    val dataCollectorLabel: String,
    val dataCollectorName: String,
    val userEmail: String,
  )

  /** The distinct, non-empty photo filenames referenced by the document's table rows. */
  fun photoFilenames(): Set<String> =
    table.rows
      .mapNotNull { (it.answer as? Answer.Photo)?.remoteFilename }
      .filter { it.isNotEmpty() }
      .toSet()
}
