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
package org.groundplatform.domain.usecases.survey

/**
 * Parses a scanned QR code and returns the encoded survey ID, or `null` if there isn't a valid
 * survey deep link.
 */
class ParseSurveyQrCodeUseCase(
  private val deepLinkHost: String,
  private val deepLinkPath: String,
) {

  operator fun invoke(payload: String): String? {
    val regex =
      Regex(
        """^https?://${Regex.escape(deepLinkHost)}${Regex.escape(deepLinkPath)}([A-Za-z0-9_-]+)/?$"""
      )
    return regex.matchEntire(payload.trim())?.groupValues?.getOrNull(1)
  }
}
