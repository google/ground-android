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
package org.groundplatform.domain.model.qrscanner
sealed interface QrScanResult {
  /** The scanner returned a decoded payload. */
  data class Success(val text: String) : QrScanResult

  /** The user dismissed the scanner without a successful scan. */
  data object Cancelled : QrScanResult

  /** The scan failed (camera unavailable, module install failure, etc.). */
  data class Error(val cause: Throwable) : QrScanResult
}
