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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.groundplatform.domain.model.settings.MeasurementUnits
import org.groundplatform.domain.model.settings.UserSettings
import org.groundplatform.domain.usecases.user.GetUserSettingsUseCase
import org.groundplatform.domain.usecases.user.UpdateUserSettingsUseCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  @Mock lateinit var getUserSettingsUseCase: GetUserSettingsUseCase
  @Mock lateinit var updateUserSettingsUseCase: UpdateUserSettingsUseCase

  private lateinit var viewModel: SettingsViewModel

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `uiState is populated with initial user settings`() = runTest {
    val userSettings = UserSettings("en", MeasurementUnits.METRIC, false)
    `when`(getUserSettingsUseCase()).thenReturn(userSettings)

    viewModel = SettingsViewModel(getUserSettingsUseCase, updateUserSettingsUseCase)

    assertThat(viewModel.uiState.value).isEqualTo(userSettings)
  }

  @Test
  fun `updateSelectedLanguage updates use case and uiState`() = runTest {
    val initialSettings = UserSettings("en", MeasurementUnits.METRIC, false)
    `when`(getUserSettingsUseCase()).thenReturn(initialSettings)
    viewModel = SettingsViewModel(getUserSettingsUseCase, updateUserSettingsUseCase)

    viewModel.updateSelectedLanguage("fr")

    val expectedSettings = initialSettings.copy(language = "fr")
    verify(updateUserSettingsUseCase).invoke(expectedSettings)
    assertThat(viewModel.uiState.value.language).isEqualTo("fr")
  }

  @Test
  fun `updateMeasurementUnits updates use case and uiState`() = runTest {
    val initialSettings = UserSettings("en", MeasurementUnits.METRIC, false)
    `when`(getUserSettingsUseCase()).thenReturn(initialSettings)
    viewModel = SettingsViewModel(getUserSettingsUseCase, updateUserSettingsUseCase)

    viewModel.updateMeasurementUnits(MeasurementUnits.IMPERIAL)

    val expectedSettings = initialSettings.copy(measurementUnits = MeasurementUnits.IMPERIAL)
    verify(updateUserSettingsUseCase).invoke(expectedSettings)
    assertThat(viewModel.uiState.value.measurementUnits).isEqualTo(MeasurementUnits.IMPERIAL)
  }

  @Test
  fun `updateUploadMediaOverUnmeteredConnectionOnly updates use case and uiState`() = runTest {
    val initialSettings = UserSettings("en", MeasurementUnits.METRIC, false)
    `when`(getUserSettingsUseCase()).thenReturn(initialSettings)
    viewModel = SettingsViewModel(getUserSettingsUseCase, updateUserSettingsUseCase)

    viewModel.updateUploadMediaOverUnmeteredConnectionOnly(true)

    val expectedSettings = initialSettings.copy(shouldUploadPhotosOnWifiOnly = true)
    verify(updateUserSettingsUseCase).invoke(expectedSettings)
    assertThat(viewModel.uiState.value.shouldUploadPhotosOnWifiOnly).isTrue()
  }
}
