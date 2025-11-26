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

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.launchFragmentWithNavController
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenMapContainerFragmentTest : BaseHiltTest() {

  private lateinit var navController: NavController
  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  override fun setUp() {
    super.setUp()
    launchFragmentWithNavController<HomeScreenMapContainerFragment>(
      destId = R.id.home_screen_fragment,
      navControllerCallback = { navController = it },
    )
  }

  @Test
  fun `Click map type launches MapTypeDialogFragment`() = runWithTestDispatcher {
    composeTestRule.onNodeWithTag(MapFloatingActionButtonType.MapType.testTag).performClick()
    advanceUntilIdle()
    assertThat(navController.currentDestination?.id).isEqualTo(R.id.mapTypeDialogFragment)
  }
}
