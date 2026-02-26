/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.ui.common.AbstractViewModel

@HiltViewModel
class SettingsViewModel
@Inject
internal constructor(private val localValueStore: LocalValueStore, userRepository: UserRepository) :
  AbstractViewModel() {

  private var _uiState: MutableStateFlow<UserSettings> =
    MutableStateFlow(userRepository.getUserSettings())
  val uiState: StateFlow<UserSettings> = _uiState

  private fun updateState(newUiState: UserSettings) {
    _uiState.value = newUiState
  }

  fun updateSelectedLanguage(language: String) {
    localValueStore.selectedLanguage = language
    updateState(_uiState.value.copy(language = language))
  }

  fun updateMeasurementUnits(measurementUnits: MeasurementUnits) {
    localValueStore.selectedLengthUnit = measurementUnits.name
    updateState(_uiState.value.copy(measurementUnits = measurementUnits))
  }

  fun updateUploadMediaOverUnmeteredConnectionOnly(enabled: Boolean) {
    localValueStore.shouldUploadMediaOverUnmeteredConnectionOnly = enabled
    updateState(_uiState.value.copy(shouldUploadPhotosOnWifiOnly = enabled))
  }
}
