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

import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.usecases.user.GetUserSettingsUseCase

class SettingsViewModel
@Inject
internal constructor(
  private val getUserSettingsUseCase: GetUserSettingsUseCase,
  private val userRepository: UserRepository,
) : AbstractViewModel() {

  private val _uiState: MutableStateFlow<UserSettings?> = MutableStateFlow(null)
  val uiState: StateFlow<UserSettings?> = _uiState

  init {
    refreshUserPreferences()
  }

  fun refreshUserPreferences() {
    viewModelScope.launch {
      val prefs = getUserSettingsUseCase.invoke()
      _uiState.value = prefs
    }
  }

  fun updateUploadMediaOverUnmeteredConnectionOnly(enabled: Boolean) {
    userRepository.updateUploadMediaOverUnmeteredConnectionOnly(enabled)
    refreshUserPreferences()
  }

  fun updateMeasurementUnits(measurementUnits: MeasurementUnits) {
    userRepository.updateMeasurementUnits(measurementUnits)
    refreshUserPreferences()
  }

  fun updateSelectedLanguage(language: String) {
    userRepository.updateSelectedLanguage(language)
    refreshUserPreferences()
  }
}
