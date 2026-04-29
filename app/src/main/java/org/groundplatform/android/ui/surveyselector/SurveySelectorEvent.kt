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
package org.groundplatform.android.ui.surveyselector

sealed interface SurveySelectorEvent {
  object NavigateToHome : SurveySelectorEvent

  data class ShowError(val errorType: ErrorType) : SurveySelectorEvent

  /** Errors surfaced from the Survey Selector screen for the host to display. */
  sealed interface ErrorType {
    /** Survey loading or activation timed out. */
    data object Timeout : ErrorType

    /** A generic error encountered while loading or activating a survey. */
    data class Generic(val cause: Throwable) : ErrorType

    /** The scanned QR code did not encode a valid survey link. */
    data object InvalidQrCode : ErrorType
  }
}
