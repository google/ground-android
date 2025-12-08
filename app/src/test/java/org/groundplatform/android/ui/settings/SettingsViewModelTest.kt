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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.groundplatform.android.model.settings.UserSettings
import org.groundplatform.android.usecases.user.GetUserSettingsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  @Mock lateinit var getUserSettingsUseCase: GetUserSettingsUseCase

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
  fun `uiState is populated with correct user settings`() = runTest {
    val userSettings =
      UserSettings(language = "en", measurementUnits = "m", shouldUploadPhotosOnWifiOnly = false)
    whenever(getUserSettingsUseCase.invoke()).thenReturn(userSettings)

    viewModel = SettingsViewModel(getUserSettingsUseCase)

    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(userSettings, viewModel.uiState.value)
    verify(getUserSettingsUseCase).invoke()
  }
}
