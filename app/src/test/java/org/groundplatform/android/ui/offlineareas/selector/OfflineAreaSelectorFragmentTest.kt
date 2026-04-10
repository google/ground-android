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
package org.groundplatform.android.ui.offlineareas.selector

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.testrules.FragmentScenarioRule
import org.groundplatform.android.ui.offlineareas.selector.model.OfflineAreaSelectorState
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorFragmentTest : BaseHiltTest() {

  lateinit var fragment: OfflineAreaSelectorFragment
  @Inject lateinit var viewModel: OfflineAreaSelectorViewModel

  private val offlineAreaRepository: OfflineAreaRepository = mock()
  @BindValue @Mock lateinit var networkManager: NetworkManager

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  @get:Rule val fragmentScenario = FragmentScenarioRule()

  @Before
  override fun setUp() {
    super.setUp()
    fragmentScenario.launchFragmentInHiltContainer<OfflineAreaSelectorFragment> {
      fragment = this as OfflineAreaSelectorFragment
    }
  }

  @Test
  fun `all the buttons are visible`() {
    onView(withId(R.id.download_button)).check(matches(isDisplayed()))
    onView(withId(R.id.cancel_button)).check(matches(isDisplayed()))
    onView(withId(R.id.cancel_button)).check(matches(isEnabled()))
  }

  @Test
  fun `default value of bottomText`() {
    onView(withId(R.id.bottom_text)).check(matches(withText("")))
  }

  @Test
  fun `toolbar text should be correct`() {
    onView(withId(R.id.offline_area_selector_toolbar))
      .check(
        matches(hasDescendant(withText(fragment.getString(R.string.offline_area_selector_title))))
      )
  }

  @Test
  fun `download button should be disabled and not clickable by default`() {
    onView(withId(R.id.download_button)).check(matches(isDisplayed()))
    onView(withId(R.id.download_button)).check(matches(not(isEnabled())))
  }

  @Test
  fun `bottom text should be empty by default`() {
    onView(withId(R.id.bottom_text)).check(matches(withText("")))
  }

  // TODO: Complete below test
  // Issue URL: https://github.com/google/ground-android/issues/3032
  @Test
  fun `stopDownloading cancels active download and updates UI state`() = runWithTestDispatcher {
    composeTestRule.setContent { DownloadProgressDialog(0f, {}) }

    val progressFlow = MutableSharedFlow<Pair<Int, Int>>()
    whenever(offlineAreaRepository.downloadTiles(any())).thenReturn(progressFlow)

    viewModel.onDownloadClick()
    advanceUntilIdle()

    progressFlow.emit(Pair(50, 100))
    advanceUntilIdle()

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .performClick()
    progressFlow.emit(Pair(75, 100))

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.cancel))
      .isNotDisplayed()

    val state = viewModel.uiState.value
    assert(state.downloadState is OfflineAreaSelectorState.DownloadState.Idle)
    assert(viewModel.downloadJob == null)
  }

  // TODO: Write `test test failure case displays toast`
  // Issue URL: https://github.com/google/ground-android/issues/3038
}
