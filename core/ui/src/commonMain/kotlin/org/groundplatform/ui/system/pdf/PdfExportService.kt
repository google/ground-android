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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.groundplatform.ui.model.SubmissionPdfDocument
import org.groundplatform.ui.model.SubmissionPdfDocument.Answer
import org.groundplatform.ui.system.pdf.image.PdfImageProvider
import org.groundplatform.ui.system.pdf.io.PdfOutputProvider
import org.groundplatform.ui.system.pdf.io.PdfReportLauncher

/**
 * Shared entry point for the PDF export flow.
 *
 * Loads images, renders the document to disk, then opens or shares the file using
 * [PdfReportLauncher].
 */
class PdfExportService(
  private val imageProvider: PdfImageProvider,
  private val renderer: PdfRenderer,
  private val outputProvider: PdfOutputProvider,
  private val launcher: PdfReportLauncher,
  private val coroutineDispatcher: CoroutineDispatcher,
) {
  private val mutex = Mutex()

  suspend fun export(request: Request, action: Action) {
    val outputPath = mutex.withLock {
      withContext(coroutineDispatcher) {
        outputProvider.pruneOldFiles()
        val path = outputProvider.newFilePath(request.fileName)
        if (!outputProvider.exists(request.fileName)) {
          val images = imageProvider.load(request.qrContent, request.document.photoFilenames())
          try {
            renderer.render(request.document, images, path)
          } finally {
            images.release()
          }
        }
        path
      }
    }
    when (action) {
      Action.Open -> launcher.open(outputPath)
      Action.Share -> launcher.share(outputPath)
    }
  }

  enum class Action {
    Open,
    Share,
  }

  data class Request(
    val document: SubmissionPdfDocument,
    val qrContent: String?,
    val fileName: String,
  )
}

/** The distinct, non-empty photo filenames referenced by the document's table rows. */
private fun SubmissionPdfDocument.photoFilenames(): Set<String> =
  table.rows
    .mapNotNull { (it.answer as? Answer.Photo)?.remoteFilename }
    .filter { it.isNotEmpty() }
    .toSet()
