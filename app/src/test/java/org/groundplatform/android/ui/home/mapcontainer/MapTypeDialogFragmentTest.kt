/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.android.ui.home.mapcontainer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.bundleOf
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.launchFragmentInHiltContainer
import org.groundplatform.android.model.map.MapType
import org.groundplatform.android.repository.MapStateRepository
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper.idleMainLooper
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MapTypeDialogFragmentTest : BaseHiltTest() {

  @Inject lateinit var mapStateRepository: MapStateRepository

  private lateinit var fragment: MapTypeDialogFragment

  @Before
  override fun setUp() {
    super.setUp()
    setupFragment()
  }

  @Test
  fun `renders dialog correctly`() {
    assertThat(fragment.isVisible).isTrue()

    composeTestRule.onNodeWithText("Layers").assertIsDisplayed()
    composeTestRule.onNodeWithText("Base map").assertIsDisplayed()
    composeTestRule.onNodeWithText("Offline map imagery").assertIsDisplayed()

    composeTestRule.onNodeWithText("Road map").assertIsDisplayed()
    composeTestRule.onNodeWithText("Terrain").assertIsDisplayed()
    composeTestRule.onNodeWithText("Satellite").assertIsDisplayed()
  }

  @Test
  fun `dismiss dialog after map type selection`() {
    assertThat(fragment.isVisible).isTrue()

    composeTestRule.onNodeWithText("Terrain").performClick()

    // Allow Robolectric to process the dismiss transaction
    idleMainLooper()

    assertThat(fragment.isVisible).isFalse()
  }

  @Test
  fun `displays default map type correctly`() {
    assertThat(mapStateRepository.mapType).isEqualTo(MapType.TERRAIN)
  }

  @Test
  fun `changes map type when selected`() {
    composeTestRule.onNodeWithText("Road map").performClick()

    assertThat(mapStateRepository.mapType).isEqualTo(MapType.ROAD)
  }

  private fun setupFragment() {
    launchFragmentInHiltContainer<MapTypeDialogFragment>(
      bundleOf(Pair("mapTypes", MapType.entries.toTypedArray()))
    ) {
      fragment = this as MapTypeDialogFragment
    }
  }
}
