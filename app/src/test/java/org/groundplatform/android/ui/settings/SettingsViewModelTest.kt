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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.groundplatform.android.repository.UserRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  @Mock lateinit var userRepository: UserRepository

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

  //  @Test
  //  fun `uiState is populated with correct user settings`() = runTest {
  //    val userSettings =
  //      UserSettings(
  //        language = "en",
  //        measurementUnits = MeasurementUnits.METRIC,
  //        shouldUploadPhotosOnWifiOnly = false,
  //      )
  //    doReturn(flowOf(userSettings)).`when`(userRepository).userSettingsFlow
  //
  //    viewModel = SettingsViewModel(userRepository)
  //
  //    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
  // viewModel.uiState.collect() }
  //
  //    testDispatcher.scheduler.advanceUntilIdle()
  //
  //    assertEquals(userSettings, viewModel.uiState.value)
  //  }

  //  @Test
  //  fun `Refreshing preferences updates the uiState again`() = runTest {
  //    val settings1 = UserSettings("en", MeasurementUnits.METRIC, false)
  //    val settings2 = UserSettings("fr", MeasurementUnits.IMPERIAL, true)
  //
  //    val flow = MutableSharedFlow<UserSettings>(replay = 1)
  //    flow.emit(settings1)
  //
  //    doReturn(flow).`when`(userRepository).userSettingsFlow
  //
  //    viewModel = SettingsViewModel(userRepository)
  //
  //    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
  // viewModel.uiState.collect() }
  //    testDispatcher.scheduler.advanceUntilIdle()
  //    assertEquals(settings1, viewModel.uiState.value)
  //
  //    flow.emit(settings2)
  //    testDispatcher.scheduler.advanceUntilIdle()
  //
  //    assertEquals(settings2, viewModel.uiState.value)
  //  }
}
