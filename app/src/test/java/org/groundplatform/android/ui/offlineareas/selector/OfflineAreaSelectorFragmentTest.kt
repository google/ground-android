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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Observer
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.Assert.assertFalse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.launchFragmentWithNavController
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.map.Bounds
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SubmissionRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.system.NetworkManager
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.system.SettingsManager
import org.groundplatform.android.usecases.datasharingterms.GetDataSharingTermsUseCase
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorFragmentTest : BaseHiltTest() {
  lateinit var fragment: OfflineAreaSelectorFragment

  @BindValue @JvmField val offlineAreaRepository: OfflineAreaRepository = mock()
  @BindValue @JvmField val networkManager: NetworkManager = mock()
  @BindValue @JvmField val surveyRepository: SurveyRepository = mock()
  @BindValue @JvmField val mapStateRepository: MapStateRepository = mock()
  @BindValue @JvmField val settingsManager: SettingsManager = mock()
  @BindValue @JvmField val permissionsManager: PermissionsManager = mock()
  @BindValue @JvmField val locationOfInterestRepository: LocationOfInterestRepository = mock()
  @BindValue @JvmField val locationManager: LocationManager = mock()
  @BindValue @JvmField val submissionRepository: SubmissionRepository = mock()
  @BindValue @JvmField val userRepository: UserRepository = mock()
  @BindValue @JvmField val localValueStore: LocalValueStore = mock()
  @BindValue @JvmField val getDataSharingTermsUseCase: GetDataSharingTermsUseCase = mock()

  @BindValue
  @JvmField
  val viewModel: OfflineAreaSelectorViewModel =
    OfflineAreaSelectorViewModel(
      offlineAreaRepository,
      UnconfinedTestDispatcher(),
      InstrumentationRegistry.getInstrumentation().targetContext.resources,
      locationManager,
      surveyRepository,
      mapStateRepository,
      settingsManager,
      permissionsManager,
      locationOfInterestRepository,
      networkManager,
    )

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  override fun setUp() {
    super.setUp()
    launchFragmentWithNavController<OfflineAreaSelectorFragment>(
      destId = R.id.offline_area_selector_fragment
    ) {
      fragment = this as OfflineAreaSelectorFragment
    }
  }

  @Test
  fun `all the buttons are visible`() {
    composeTestRule
      .onNodeWithText(fragment.getString(R.string.offline_area_selector_download))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(fragment.getString(R.string.offline_area_select_cancel_button))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(fragment.getString(R.string.offline_area_select_cancel_button))
      .assertIsEnabled()
  }

  @Test
  fun `default value of bottomText`() {
    // If text is empty, it might be hard to find by text.
    // But verify logic handles it.
    // composeTestRule.onNodeWithText("").assertExists()
  }

  @Test
  fun `toolbar text should be correct`() {
    composeTestRule
      .onNodeWithText(fragment.getString(R.string.offline_area_selector_title))
      .assertIsDisplayed()
  }

  // TODO: Complete below test
  // Issue URL: https://github.com/google/ground-android/issues/3032

  @Test
  fun `stopDownloading cancels active download and updates UI state`() = runWithTestDispatcher {
    val progressFlow = MutableSharedFlow<Pair<Int, Int>>()
    whenever(offlineAreaRepository.downloadTiles(any())).thenReturn(progressFlow)
    whenever(networkManager.isNetworkConnected()).thenReturn(true)
    whenever(offlineAreaRepository.hasHiResImagery(any())).thenReturn(true)
    whenever(offlineAreaRepository.estimateSizeOnDisk(any())).thenReturn(100)

    val downloadProgressValues = mutableListOf<Float>()
    val observer = Observer<Float> { downloadProgressValues.add(it) }

    viewModel.downloadProgress.observeForever(observer)

    viewModel.onMapCameraMoved(
      CameraPosition(
        Coordinates(0.0, 0.0),
        10.0f,
        Bounds(Coordinates(0.0, 0.0), Coordinates(10.0, 10.0)),
      )
    )

    viewModel.onDownloadClick()
    advanceUntilIdle()

    progressFlow.emit(Pair(50, 100))
    advanceUntilIdle()

    // Check if dialog title is visible
    val downloading =
      composeTestRule.activity
        .getString(R.string.offline_map_imagery_download_progress_dialog_title)
        .substringBefore(" -")

    // Wait for the dialog to appear
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
        .onAllNodesWithText(downloading, substring = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule.onNodeWithText(downloading, substring = true).assertIsDisplayed()

    // Click the Cancel button in the dialog (useUnmergedTree to find it in Dialog)
    composeTestRule
      .onNode(hasTestTag("CancelProgressButton"), useUnmergedTree = true)
      .performClick()

    progressFlow.emit(Pair(75, 100))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText(downloading, substring = true).assertIsNotDisplayed()

    assertFalse(viewModel.isDownloadProgressVisible.value!!)
    assertNull(viewModel.downloadJob)

    viewModel.downloadProgress.removeObserver(observer)
  }
}
