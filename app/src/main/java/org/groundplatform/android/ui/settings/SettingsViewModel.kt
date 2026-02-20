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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.ui.common.AbstractViewModel

@HiltViewModel
class SettingsViewModel @Inject internal constructor(private val userRepository: UserRepository) :
  AbstractViewModel() {

  val uiState: StateFlow<UserSettings?> =
    userRepository.userSettingsFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

  fun updateUploadMediaOverUnmeteredConnectionOnly(enabled: Boolean) {
    userRepository.updateUploadMediaOverUnmeteredConnectionOnly(enabled)
  }

  fun updateMeasurementUnits(measurementUnits: MeasurementUnits) {
    userRepository.updateMeasurementUnits(measurementUnits)
  }

  fun updateSelectedLanguage(language: String) {
    userRepository.updateSelectedLanguage(language)
  }
}
