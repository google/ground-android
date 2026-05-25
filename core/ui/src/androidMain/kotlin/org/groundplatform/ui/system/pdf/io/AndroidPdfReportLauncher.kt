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
package org.groundplatform.ui.system.pdf.io

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

private const val PDF_MIME_TYPE = "application/pdf"

/** Launches the system share sheet or external viewer for a report file via [FileProvider]. */
class AndroidPdfReportLauncher(
  private val context: Context,
  private val fileProviderAuthority: String,
) : PdfReportLauncher {

  override fun share(path: String) {
    val uri = FileProvider.getUriForFile(context, fileProviderAuthority, File(path))
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = PDF_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    launchChooser(sendIntent)
  }

  override fun open(path: String) {
    val uri = FileProvider.getUriForFile(context, fileProviderAuthority, File(path))
    val viewIntent =
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, PDF_MIME_TYPE)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    launchChooser(viewIntent)
  }

  private fun launchChooser(target: Intent) {
    val chooser =
      Intent.createChooser(target, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(chooser)
  }
}
