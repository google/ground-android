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
package org.groundplatform.android.ui.basemapselector

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.model.map.MapType
import org.groundplatform.android.repository.MapStateRepository
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w600dp-h1024dp")
class BasemapSelectorScreenTest : BaseHiltTest() {

  @Inject lateinit var mapStateRepository: MapStateRepository

  private lateinit var viewModel: BasemapSelectorViewModel
  private var isDismissed = false

  @Before
  override fun setUp() {
    super.setUp()
    viewModel = BasemapSelectorViewModel(mapStateRepository)
    isDismissed = false
    composeTestRule.setContent {
      BasemapSelectorScreen(
        mapTypes = listOf(MapType.ROAD, MapType.TERRAIN, MapType.SATELLITE),
        onDismissRequest = { isDismissed = true },
        viewModel = viewModel,
      )
    }
  }

  @Test
  fun `renders dialog correctly`() {
    composeTestRule.onNodeWithText("Layers").assertIsDisplayed()
    composeTestRule.onNodeWithText("Base map").assertIsDisplayed()
    composeTestRule.onNodeWithText("Road map").assertIsDisplayed()
    composeTestRule.onNodeWithText("Terrain").assertIsDisplayed()
    composeTestRule.onNodeWithText("Satellite").assertIsDisplayed()
    composeTestRule.onNodeWithText("Offline map imagery").assertIsDisplayed()
  }

  @Test
  fun `displays default map type correctly`() {
    assertThat(mapStateRepository.mapType).isEqualTo(MapType.TERRAIN)
  }

  @Test
  fun `changes map type when clicked`() {
    composeTestRule.onNodeWithText("Road map").performClick()

    composeTestRule.waitForIdle()

    assertThat(mapStateRepository.mapType).isEqualTo(MapType.ROAD)
  }

  @Test
  fun `dismiss dialog after map type selection`() {
    composeTestRule.onNodeWithText("Terrain").performClick()

    composeTestRule.waitForIdle()

    assertThat(isDismissed).isTrue()
  }

  @Test
  fun `displays offline map imagery toggle when enabled`() {
    viewModel.setOfflineImageryEnabled(true)

    composeTestRule.onNode(isToggleable()).assertIsOn()
  }

  @Test
  fun `displays offline map imagery toggle when disabled`() {
    viewModel.setOfflineImageryEnabled(false)

    composeTestRule.onNode(isToggleable()).assertIsOff()
  }

  @Test
  fun `toggles offline map imagery when switched`() {
    viewModel.setOfflineImageryEnabled(false)

    composeTestRule.onNode(isToggleable()).performClick()

    composeTestRule.onNode(isToggleable()).assertIsOn()
    assertThat(mapStateRepository.isOfflineImageryEnabled).isTrue()
  }
}
