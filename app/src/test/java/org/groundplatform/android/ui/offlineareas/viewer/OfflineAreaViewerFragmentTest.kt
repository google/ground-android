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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.OFFLINE_AREA
import org.groundplatform.android.R
import org.groundplatform.android.data.local.stores.LocalOfflineAreaStore
import org.groundplatform.android.testrules.FragmentScenarioRule
import org.groundplatform.domain.model.map.MapType
import org.groundplatform.ui.map.MapConfig
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaViewerFragmentTest : BaseHiltTest() {

  @get:Rule val fragmentScenario = FragmentScenarioRule()
  @get:Rule val composeTestRule = createComposeRule()

  @Inject lateinit var localOfflineAreaStore: LocalOfflineAreaStore
  private lateinit var fragment: OfflineAreaViewerFragment
  private lateinit var navController: NavController

  @Test
  fun `RemoveButton is displayed and enable`() = runWithTestDispatcher {
    setupFragment()
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG)
      .assertIsDisplayed()
      .assertIsEnabled()
  }

  @Test
  fun `All values are correctly displayed`() = runWithTestDispatcher {
    setupFragment()
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_NAME_TEST_TAG)
      .assertTextEquals(OFFLINE_AREA.name)
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_SIZE_TEST_TAG)
      .assertTextEquals("<1\u00A0MB on disk")
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG)
      .assertTextContains(fragment.getString(R.string.offline_area_viewer_remove_button))
    composeTestRule
      .onNodeWithText(fragment.getString(R.string.offline_area_viewer_title))
      .assertIsDisplayed()
  }

  @Test
  fun `When no offline areas available`() = runWithTestDispatcher {
    setupFragmentWithoutDb()
    advanceUntilIdle()
    composeTestRule
      .onNodeWithText(fragment.getString(R.string.offline_area_viewer_title))
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag(OFFLINE_AREA_VIEWER_NAME_TEST_TAG).assertTextEquals("")
    composeTestRule.onNodeWithTag(OFFLINE_AREA_VIEWER_SIZE_TEST_TAG).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG).assertIsNotEnabled()
    composeTestRule
      .onNodeWithTag(OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG)
      .assertTextContains(fragment.getString(R.string.offline_area_viewer_remove_button))
  }

  @Test
  fun `Clicking remove button deletes area and navigates up`() = runWithTestDispatcher {
    setupFragment()
    composeTestRule.onNodeWithTag(OFFLINE_AREA_VIEWER_REMOVE_BUTTON_TEST_TAG).performClick()
    advanceUntilIdle()

    assertThat(localOfflineAreaStore.getOfflineAreaById(OFFLINE_AREA.id)).isNull()
    assertThat(navController.currentDestination?.id).isNotEqualTo(R.id.offline_area_viewer_fragment)
  }

  @Test
  fun `Clicking back button in toolbar navigates up`() = runWithTestDispatcher {
    setupFragment()
    advanceUntilIdle()

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    advanceUntilIdle()

    assertThat(navController.currentDestination?.id).isNotEqualTo(R.id.offline_area_viewer_fragment)
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

    fragmentScenario.launchFragmentWithNavController<OfflineAreaViewerFragment>(
      argsBundle,
      destId = R.id.offline_area_viewer_fragment,
    ) {
      fragment = this as OfflineAreaViewerFragment
      navController = fragment.findNavController()
    }
  }
}
