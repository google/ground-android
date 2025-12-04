/*
 * Copyright 2024 Google LLC
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

import org.groundplatform.android.model.SurveyListItem

sealed class UiState {

  /** Represents that surveys are being fetched from the remote server. */
  data object FetchingSurveys : UiState()

  /**
   * Represents that surveys have been successfully fetched and are available to be shown to the
   * user.
   */
  data class SurveyListAvailable(val surveys: List<SurveyListItem>) : UiState()

  /** Represents that the selected survey and its associated LOIs are being persisted locally. */
  data object ActivatingSurvey : UiState()

  /**
   * Represents that the selected survey and its associated LOIs have been successfully saved
   * locally and is ready to be loaded.
   */
  data object SurveyActivated : UiState()

  /** Represents that there was an error while activating surveys. */
  data class Error(val error: Throwable? = null) : UiState()

  data object NavigateToHome : UiState()
}
