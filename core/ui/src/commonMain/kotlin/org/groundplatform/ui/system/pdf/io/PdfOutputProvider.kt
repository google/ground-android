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

import kotlin.time.Clock

private const val MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000 // 1 week

/** Manages file paths and cached PDF reports. */
interface PdfOutputProvider {
  fun newFilePath(name: String): String

  fun exists(name: String): Boolean

  fun listFiles(): List<CachedPdf>

  fun deleteReport(path: String)

  /** Removes cached reports older than [MAX_AGE_MILLIS] (1 week). */
  fun pruneOldFiles() {
    val now = Clock.System.now().toEpochMilliseconds()
    listFiles()
      .filter { now - it.lastModifiedMillis > MAX_AGE_MILLIS }
      .forEach { deleteReport(it.path) }
  }

  /** A cached PDF file entry with its path and last-modified timestamp. */
  data class CachedPdf(val path: String, val lastModifiedMillis: Long)
}
