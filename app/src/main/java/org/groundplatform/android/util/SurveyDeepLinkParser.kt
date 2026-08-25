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
package org.groundplatform.android.util

import android.net.Uri
import androidx.core.net.toUri
import org.groundplatform.android.common.Constants.SURVEY_PATH_SEGMENT

/**
 * Parses a scanned QR code and returns the encoded survey ID, or `null` if there isn't a valid
 * survey deep link.
 *
 * @param deepLinkHost The host of the deep link URL (e.g. "groundplatform.org")
 * @param deepLinkPath The path of the deep link URL (e.g. "/android/survey/")
 */
class SurveyDeepLinkParser(private val deepLinkHost: String, private val deepLinkPath: String) {

  /** Parses a raw payload [String] (e.g. text scanned from a QR code). */
  fun parse(payload: String): String? {
    val uri = runCatching { payload.trim().toUri() }.getOrNull() ?: return null
    return parse(uri)
  }

  /** Parses an [Uri] (e.g. an incoming deep-link intent's data). */
  fun parse(uri: Uri): String? {
    if (!uri.isValidDeepLink()) return null
    val segments = uri.pathSegments
    return segments
      .indexOf(SURVEY_PATH_SEGMENT)
      .takeIf { it != -1 }
      ?.let { segments.getOrNull(it + 1) }
      .takeIf { !it.isNullOrBlank() }
  }

  private fun Uri.isValidDeepLink(): Boolean =
    scheme == "https" && host == deepLinkHost && path?.startsWith(deepLinkPath) == true
}
