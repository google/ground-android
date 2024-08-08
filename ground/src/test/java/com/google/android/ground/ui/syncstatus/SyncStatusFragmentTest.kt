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
package com.google.android.ground.ui.syncstatus

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.repository.SurveyRepository
import com.sharedtest.FakeData.SURVEY
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SyncStatusFragmentTest : BaseHiltTest() {

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var surveyRepository: SurveyRepository

  override fun setUp() {
    super.setUp()
    launchFragmentInHiltContainer<SyncStatusFragment>()
  }

  @Test
  fun `Toolbar should be displayed`() {
    onView(withId(R.id.sync_status_toolbar)).check(matches(isDisplayed()))
  }

  @Test
  fun `Sync items should be displayed`() = runWithTestDispatcher {
    fakeRemoteDataStore.surveys = listOf(SURVEY)
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    surveyRepository.selectedSurveyId = SURVEY.id
    advanceUntilIdle()

    composeTestRule.onNodeWithTag("sync list").assertIsDisplayed()
  }

  @Test
  fun `Sync items should not be displayed`() {
    composeTestRule.onNodeWithTag("sync list").assertIsNotDisplayed()
  }
}
