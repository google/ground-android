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

import org.groundplatform.android.R
import org.groundplatform.android.model.SurveyListItem

/** Represents a section of surveys in the survey list. */
data class SurveySection(val titleResId: Int, val surveys: List<SurveyListItem>)

/**
 * Represents the UI state of the Survey Selector screen.
 *
 * @property isLoading Whether the survey list is currently loading or a survey is being activated.
 * @property onDeviceSurveys List of surveys available offline on the device.
 * @property sharedSurveys List of surveys shared with the user.
 * @property publicSurveys List of public surveys available.
 */
data class SurveySelectorUiState(
  val isLoading: Boolean = false,
  private val onDeviceSurveys: List<SurveyListItem> = emptyList(),
  private val sharedSurveys: List<SurveyListItem> = emptyList(),
  private val publicSurveys: List<SurveyListItem> = emptyList(),
) {

  /** Returns true if the empty screen should be shown. */
  val showEmptyState: Boolean
    get() = !isLoading && !hasSurveys

  /** Returns true if surveys are available. */
  val hasSurveys: Boolean
    get() = onDeviceSurveys.isNotEmpty() || sharedSurveys.isNotEmpty() || publicSurveys.isNotEmpty()

  /** Returns a list of sections containing the title resource ID and the list of surveys. */
  val surveySections: List<SurveySection>
    get() = buildList {
      add(SurveySection(R.string.section_on_device, onDeviceSurveys))
      add(SurveySection(R.string.section_shared_with_me, sharedSurveys))
      add(SurveySection(R.string.section_public, publicSurveys))
    }
}
