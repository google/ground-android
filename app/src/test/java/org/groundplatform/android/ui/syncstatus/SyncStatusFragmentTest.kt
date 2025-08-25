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
package org.groundplatform.android.ui.syncstatus

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.FakeData.USER
import org.groundplatform.android.FakeData.newLoiMutation
import org.groundplatform.android.FakeData.newSubmissionMutation
import org.groundplatform.android.R
import org.groundplatform.android.data.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.data.local.stores.LocalSubmissionStore
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.data.local.stores.LocalUserStore
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.launchFragmentInHiltContainer
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.repository.SurveyRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SyncStatusFragmentTest : BaseHiltTest() {

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var localLoiStore: LocalLocationOfInterestStore
  @Inject lateinit var localSubmissionStore: LocalSubmissionStore
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var localUserStore: LocalUserStore
  @Inject lateinit var surveyRepository: SurveyRepository

  @Test
  fun `Toolbar should be displayed`() {
    setupFragment()

    onView(withId(R.id.sync_status_toolbar)).check(matches(isDisplayed()))
  }

  @Test
  fun `Sync items should be displayed`() = runWithTestDispatcher {
    setupSurvey()
    setupFragment()

    composeTestRule.onNodeWithTag("sync list").assertIsDisplayed()
  }

  @Test
  fun `Entry for LOI Mutation is displayed`() = runWithTestDispatcher {
    setupSurvey()

    // Insert a new LOI mutation in local db
    localUserStore.insertOrUpdateUser(USER)
    localLoiStore.applyAndEnqueue(newLoiMutation(point = Point(Coordinates(0.0, 0.0))))
    advanceUntilIdle()

    setupFragment()

    composeTestRule.onNodeWithTag("sync list").assertIsDisplayed()
    composeTestRule.onNodeWithText("User").assertIsDisplayed() // User's display name
    composeTestRule.onNodeWithText("Pending").assertIsDisplayed() // Status
    composeTestRule.onNodeWithText("Job").assertIsDisplayed() // Label
    composeTestRule.onNodeWithText("Test LOI Name").assertIsDisplayed() // Subtitle
  }

  @Test
  fun `Entry for Submission Mutation is displayed`() = runWithTestDispatcher {
    setupSurvey()

    // Insert a new submission mutation in local db
    localUserStore.insertOrUpdateUser(USER)
    localLoiStore.apply(newLoiMutation(point = Point(Coordinates(0.0, 0.0))))
    localSubmissionStore.applyAndEnqueue(newSubmissionMutation())
    advanceUntilIdle()

    setupFragment()

    composeTestRule.onNodeWithTag("sync list").assertIsDisplayed()
    composeTestRule.onNodeWithText("User").assertIsDisplayed() // User's display name
    composeTestRule.onNodeWithText("Pending").assertIsDisplayed() // Status
    composeTestRule.onNodeWithText("Job").assertIsDisplayed() // Label
    composeTestRule.onNodeWithText("Survey title").assertIsDisplayed() // Subtitle
  }

  private fun setupSurvey() = runWithTestDispatcher {
    fakeRemoteDataStore.surveys = listOf(SURVEY)
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    surveyRepository.activateSurvey(SURVEY.id)
    advanceUntilIdle()
  }

  private fun setupFragment() = runWithTestDispatcher {
    launchFragmentInHiltContainer<SyncStatusFragment>()
    advanceUntilIdle()
  }
}
