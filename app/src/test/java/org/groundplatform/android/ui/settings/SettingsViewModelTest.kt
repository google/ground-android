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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.repository.UserRepository
import org.junit.After
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

  @Mock lateinit var userRepository: UserRepository
  @Mock lateinit var localValueStore: LocalValueStore

  private lateinit var viewModel: SettingsViewModel

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `uiState is populated with initial user settings`() = runTest {
    val userSettings = UserSettings("en", MeasurementUnits.METRIC, false)
    `when`(userRepository.getUserSettings()).thenReturn(userSettings)

    viewModel = SettingsViewModel(localValueStore, userRepository)

    assertThat(viewModel.uiState.value).isEqualTo(userSettings)
  }

  @Test
  fun `updateSelectedLanguage updates local store and uiState`() = runTest {
    val initialSettings = UserSettings("en", MeasurementUnits.METRIC, false)
    `when`(userRepository.getUserSettings()).thenReturn(initialSettings)
    viewModel = SettingsViewModel(localValueStore, userRepository)

    viewModel.updateSelectedLanguage("fr")

    verify(localValueStore).selectedLanguage = "fr"
    assertThat(viewModel.uiState.value.language).isEqualTo("fr")
  }

  @Test
  fun `updateMeasurementUnits updates local store and uiState`() = runTest {
    val initialSettings = UserSettings("en", MeasurementUnits.METRIC, false)
    `when`(userRepository.getUserSettings()).thenReturn(initialSettings)
    viewModel = SettingsViewModel(localValueStore, userRepository)

    viewModel.updateMeasurementUnits(MeasurementUnits.IMPERIAL)

    verify(localValueStore).selectedLengthUnit = MeasurementUnits.IMPERIAL.name
    assertThat(viewModel.uiState.value.measurementUnits).isEqualTo(MeasurementUnits.IMPERIAL)
  }

  @Test
  fun `updateUploadMediaOverUnmeteredConnectionOnly updates local store and uiState`() = runTest {
    val initialSettings = UserSettings("en", MeasurementUnits.METRIC, false)
    `when`(userRepository.getUserSettings()).thenReturn(initialSettings)
    viewModel = SettingsViewModel(localValueStore, userRepository)

    viewModel.updateUploadMediaOverUnmeteredConnectionOnly(true)

    verify(localValueStore).shouldUploadMediaOverUnmeteredConnectionOnly = true
    assertThat(viewModel.uiState.value.shouldUploadPhotosOnWifiOnly).isTrue()
  }
}
