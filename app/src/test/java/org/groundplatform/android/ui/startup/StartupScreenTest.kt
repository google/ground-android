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
package org.groundplatform.android.ui.startup

import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class StartupScreenTest : BaseHiltTest() {

  @Mock private lateinit var mockViewModel: StartupViewModel

  @Test
  fun `Loading state shows loading dialog`() {
    setState(StartupState.Loading)

    composeTestRule.setContent { StartupScreen(onLoadFailed = {}, viewModel = mockViewModel) }

    val loadingText = RuntimeEnvironment.getApplication().getString(R.string.initializing)
    composeTestRule.onNodeWithText(loadingText).assertExists()
  }

  @Test
  fun `Error state invokes onLoadFailed`() {
    var onLoadFailedCalled = false
    setState(StartupState.Error(null))

    composeTestRule.setContent {
      StartupScreen(onLoadFailed = { onLoadFailedCalled = true }, viewModel = mockViewModel)
    }

    assertThat(onLoadFailedCalled).isTrue()
  }

  private fun setState(state: StartupState) {
    whenever(mockViewModel.state).thenReturn(MutableStateFlow(state))
  }
}
