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
package org.groundplatform.android.ui.offlineareas.viewer

import android.os.Bundle
import androidx.compose.ui.test.*
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.OFFLINE_AREA
import org.groundplatform.android.R
import org.groundplatform.android.data.local.stores.LocalOfflineAreaStore
import org.groundplatform.android.launchFragmentWithNavController
import org.groundplatform.android.model.map.MapType
import org.groundplatform.android.ui.common.MapConfig
import org.groundplatform.android.util.view.isGone
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaViewerFragmentTest : BaseHiltTest() {

  @Inject lateinit var localOfflineAreaStore: LocalOfflineAreaStore
  private lateinit var fragment: OfflineAreaViewerFragment

  @Test
  fun `RemoveButton is displayed and enabled`() = runWithTestDispatcher {
    setupFragment()
    composeTestRule.onNodeWithText(fragment.getString(R.string.offline_area_viewer_remove_button)).assertIsDisplayed().assertIsEnabled()
  }

  @Test
  fun `All values are correctly displayed`() = runWithTestDispatcher {
    setupFragment()
    composeTestRule.onNodeWithText(OFFLINE_AREA.name).assertIsDisplayed()
    composeTestRule.onNodeWithText("<1\u00A0MB on disk").assertIsDisplayed()
    composeTestRule.onNodeWithText(fragment.getString(R.string.offline_area_viewer_remove_button)).assertIsDisplayed()
    composeTestRule.onNodeWithText(fragment.getString(R.string.offline_area_viewer_title)).assertIsDisplayed()
  }

  @Test
  fun `When no offline areas available`() = runWithTestDispatcher {
    setupFragmentWithoutDb()
    advanceUntilIdle()
    composeTestRule.onNodeWithText(fragment.getString(R.string.offline_area_viewer_title)).assertIsDisplayed()
    // Name is empty string, finding empty string text node might match many or root?
    // If areaName is empty, Screen shows nothing? No, Screen shows Text(areaName).
    // If Text("") is rendered, it exists but is invisible?
    // Use onNode with text ""?
    // However, areaSize Text is CONDITIONAL on isNotEmpty(). So it should NOT exist.
    composeTestRule.onNodeWithText("<1\u00A0MB on disk").assertDoesNotExist()

    // Remove button disabled
    composeTestRule.onNodeWithText(fragment.getString(R.string.offline_area_viewer_remove_button)).assertIsDisplayed().assertIsNotEnabled()
  }

  @Test
  fun `default mapConfig value should be correct`() {
    setupFragment()

    assertThat(fragment.getMapConfig())
      .isEqualTo(
        MapConfig(
          allowGestures = false,
          overrideMapType = MapType.TERRAIN,
          showOfflineImagery = true,
        )
      )
  }

  private fun setupFragment() = runWithTestDispatcher {
    localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)
    setupFragmentWithoutDb()
  }

  private fun setupFragmentWithoutDb(fragmentArgs: Bundle? = null) = runWithTestDispatcher {
    val argsBundle =
      fragmentArgs ?: OfflineAreaViewerFragmentArgs.Builder("id_1").build().toBundle()

    launchFragmentWithNavController<OfflineAreaViewerFragment>(
      argsBundle,
      destId = R.id.offline_area_viewer_fragment,
    ) {
      fragment = this as OfflineAreaViewerFragment
    }
  }
}
