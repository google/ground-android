/*
 * Copyright 2018 Google LLC
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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.test.Test
import kotlin.test.assertTrue
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.ADHOC_JOB
import org.groundplatform.android.R
import org.groundplatform.android.ui.components.LOCATION_LOCKED_TEST_TAG
import org.groundplatform.android.ui.components.LOCATION_NOT_LOCKED_TEST_TAG
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.groundplatform.android.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentState
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenMapContainerScreenTest : BaseHiltTest() {

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `Should show all map actions button when shouldShowMapActions = true`() {
    setContent(shouldShowMapActions = true)

    composeTestRule
      .onNodeWithTag(MapFloatingActionButtonType.OpenNavDrawer.testTag)
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapFloatingActionButtonType.MapType.testTag).assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(MapFloatingActionButtonType.LocationNotLocked.testTag)
      .assertIsDisplayed()
  }

  @Test
  fun `Should not show any map actions button when shouldShowMapActions = false`() {
    setContent(shouldShowMapActions = false)

    composeTestRule
      .onNodeWithTag(MapFloatingActionButtonType.OpenNavDrawer.testTag)
      .assertIsNotDisplayed()
    composeTestRule
      .onNodeWithTag(MapFloatingActionButtonType.MapType.testTag)
      .assertIsNotDisplayed()
    composeTestRule
      .onNodeWithTag(MapFloatingActionButtonType.LocationNotLocked.testTag)
      .assertIsNotDisplayed()
  }

  @Test
  fun `Should display the correct icon when the location is not locked`() {
    setContent(locationLockButtonType = MapFloatingActionButtonType.LocationNotLocked)

    composeTestRule.onNodeWithTag(LOCATION_NOT_LOCKED_TEST_TAG).assertIsDisplayed()
  }

  @Test
  fun `Should display the correct icon the location is locked`() {
    setContent(locationLockButtonType = MapFloatingActionButtonType.LocationLocked())

    composeTestRule.onNodeWithTag(LOCATION_LOCKED_TEST_TAG).assertIsDisplayed()
  }

  @Test
  fun `Should display the recenter button`() {
    setContent(shouldShowRecenterButton = true)

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.recenter))
      .assertIsDisplayed()
  }

  @Test
  fun `Should not display the recenter button`() {
    setContent(shouldShowRecenterButton = false)

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.recenter))
      .assertIsNotDisplayed()
  }

  @Test
  fun `Should trigger OnOpenNavDrawerClicked when the correct button is clicked`() {
    val performedActions = mutableListOf<BaseMapAction>()
    setContent(onBaseMapAction = { performedActions += it })

    composeTestRule.onNodeWithTag(MapFloatingActionButtonType.OpenNavDrawer.testTag).performClick()

    assertTrue(performedActions.contains(BaseMapAction.OnOpenNavDrawerClicked))
    assert(performedActions.size == 1)
  }

  @Test
  fun `Should trigger OnMapTypeClicked when the correct button is clicked`() {
    val performedActions = mutableListOf<BaseMapAction>()
    setContent(onBaseMapAction = { performedActions += it })

    composeTestRule.onNodeWithTag(MapFloatingActionButtonType.MapType.testTag).performClick()

    assertTrue(performedActions.contains(BaseMapAction.OnMapTypeClicked))
    assert(performedActions.size == 1)
  }

  @Test
  fun `Should trigger OnLocationLockClicked when the location button is clicked`() {
    val performedActions = mutableListOf<BaseMapAction>()
    setContent(onBaseMapAction = { performedActions += it })

    composeTestRule
      .onNodeWithTag(MapFloatingActionButtonType.LocationNotLocked.testTag)
      .performClick()

    assertTrue(performedActions.contains(BaseMapAction.OnLocationLockClicked))
    assert(performedActions.size == 1)
  }

  @Test
  fun `Should trigger OnLocationLockClicked when the recenter button is clicked`() {
    val performedActions = mutableListOf<BaseMapAction>()
    setContent(onBaseMapAction = { performedActions += it })

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.recenter))
      .performClick()

    assertTrue(performedActions.contains(BaseMapAction.OnLocationLockClicked))
    assert(performedActions.size == 1)
  }

  @Test
  fun `Should forward JobComponentActions correctly`() {
    val performedActions = mutableListOf<JobMapComponentAction>()
    setContent(
      jobComponentState =
        JobMapComponentState(
          adHocDataCollectionButtonData =
            listOf(AdHocDataCollectionButtonData(canCollectData = true, job = ADHOC_JOB))
        ),
      onJobComponentAction = { performedActions += it },
    )

    composeTestRule
      .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.add_site))
      .performClick()

    assertTrue(performedActions.last() is JobMapComponentAction.OnJobSelected)
  }

  private fun setContent(
    locationLockButtonType: MapFloatingActionButtonType =
      MapFloatingActionButtonType.LocationNotLocked,
    shouldShowMapActions: Boolean = true,
    shouldShowRecenterButton: Boolean = true,
    jobComponentState: JobMapComponentState = JobMapComponentState(),
    onBaseMapAction: (BaseMapAction) -> Unit = {},
    onJobComponentAction: (JobMapComponentAction) -> Unit = {},
  ) {
    composeTestRule.setContent {
      HomeScreenMapContainerScreen(
        locationLockButtonType = locationLockButtonType,
        shouldShowMapActions = shouldShowMapActions,
        shouldShowRecenter = shouldShowRecenterButton,
        jobComponentState = jobComponentState,
        onBaseMapAction = onBaseMapAction,
        onJobComponentAction = onJobComponentAction,
      )
    }
  }
}
