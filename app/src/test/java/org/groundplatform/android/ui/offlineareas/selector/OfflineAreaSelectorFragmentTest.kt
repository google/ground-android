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

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.testrules.FragmentScenarioRule
import org.groundplatform.android.ui.offlineareas.selector.model.OfflineAreaSelectorState
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.model.map.CameraPosition
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorFragmentTest : BaseHiltTest() {

  lateinit var fragment: OfflineAreaSelectorFragment
  lateinit var viewModel: OfflineAreaSelectorViewModel
  lateinit var navController: NavController

  @BindValue @Mock lateinit var offlineAreaRepository: OfflineAreaRepository
  @BindValue @Mock lateinit var networkManager: NetworkManager

  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule val fragmentScenario = FragmentScenarioRule()

  @Before
  override fun setUp() {
    super.setUp()
    fragmentScenario.launchFragmentWithNavController<OfflineAreaSelectorFragment>(
      destId = R.id.offline_area_selector_fragment,
      navControllerCallback = { navController = it },
    ) {
      fragment = this as OfflineAreaSelectorFragment
      viewModel = ViewModelProvider(fragment)[OfflineAreaSelectorViewModel::class.java]
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

  @Test
  fun `stopDownloading cancels active download and updates UI state`() = runWithTestDispatcher {
    val progressFlow = MutableSharedFlow<Pair<Int, Int>>()
    setupMocks(downloadProgressFlow = progressFlow)
    composeTestRule.setContent { DownloadProgressDialog(0f, {}) }

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()

    onView(withId(R.id.download_button))
      .check(matches(isDisplayed()))
      .check(matches(isEnabled()))
      .perform(click())
    advanceUntilIdle()

    composeTestRule
      .onNodeWithText(getString(R.string.offline_map_imagery_download_progress_dialog_message))
      .isDisplayed()

    progressFlow.emit(Pair(50, 100))
    advanceUntilIdle()

    composeTestRule.onNodeWithText(getString(R.string.cancel)).performClick()
    progressFlow.emit(Pair(75, 100))

    composeTestRule.onNodeWithText(getString(R.string.cancel)).isNotDisplayed()

    val state = viewModel.uiState.value
    assert(state.downloadState is OfflineAreaSelectorState.DownloadState.Idle)
    assert(viewModel.downloadJob == null)
  }

  @Test
  fun `download failure displays error toast`() = runWithTestDispatcher {
    @Suppress("TooGenericExceptionThrown")
    setupMocks(downloadProgressFlow = flow { throw RuntimeException("download failed") })

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()
    onView(withId(R.id.download_button))
      .check(matches(isDisplayed()))
      .check(matches(isEnabled()))
      .perform(click())
    advanceUntilIdle()

    assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
    assertEquals(
      getString(R.string.offline_area_download_error),
      ShadowToast.getTextOfLatestToast(),
    )
  }

  @Test
  fun `network unavailable displays error popup`() = runWithTestDispatcher {
    setupMocks(isNetworkConnected = false)

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()
    onView(withId(R.id.download_button))
      .check(matches(isDisplayed()))
      .check(matches(isEnabled()))
      .perform(click())
    advanceUntilIdle()

    assertThat(ShadowToast.shownToastCount()).isEqualTo(1)
    assertEquals(
      getString(R.string.connect_to_download_message),
      ShadowToast.getTextOfLatestToast(),
    )
  }

  @Test
  fun `successful download navigates back to home screen`() = runWithTestDispatcher {
    setupMocks(downloadProgressFlow = flow { emit(Pair(100, 100)) })

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()
    onView(withId(R.id.download_button))
      .check(matches(isDisplayed()))
      .check(matches(isEnabled()))
      .perform(click())
    advanceUntilIdle()

    assertThat(navController.currentDestination!!.id).isEqualTo(R.id.home_screen_fragment)
  }

  @Test
  fun `cancel button triggers navigate up`() = runWithTestDispatcher {
    setupMocks()

    viewModel.onMapCameraMoved(CAMERA_POSITION)
    advanceUntilIdle()
    onView(withId(R.id.cancel_button)).perform(click())
    advanceUntilIdle()

    assertThat(navController.currentDestination?.id)
      .isNotEqualTo(R.id.offline_area_selector_fragment)
  }

  private suspend fun setupMocks(
    hasHiResImagery: Result<Boolean> = Result.success(true),
    estimatedSizeOnDisk: Result<Int> = Result.success(1024 * 1024 * 5),
    isNetworkConnected: Boolean = true,
    downloadProgressFlow: Flow<Pair<Int, Int>> = MutableSharedFlow(),
  ) {
    whenever(offlineAreaRepository.hasHiResImagery(any())).thenReturn(hasHiResImagery)
    whenever(offlineAreaRepository.estimateSizeOnDisk(any())).thenReturn(estimatedSizeOnDisk)
    whenever(networkManager.isNetworkConnected()).thenReturn(isNetworkConnected)
    whenever(offlineAreaRepository.downloadTiles(any())).thenReturn(downloadProgressFlow)
  }

  private companion object {
    val CAMERA_POSITION =
      CameraPosition(
        Coordinates(0.5, 0.5),
        10.0f,
        Bounds(Coordinates(0.0, 0.0), Coordinates(1.0, 1.0)),
      )
  }
}
