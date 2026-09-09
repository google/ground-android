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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.R
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.testrules.FragmentScenarioRule
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SyncStatusFragmentTest : BaseHiltTest() {
  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule val fragmentScenario = FragmentScenarioRule()

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var surveyRepository: SurveyRepositoryInterface

  @Test
  fun `Sync items should be displayed`() = runWithTestDispatcher {
    setupSurvey()
    setupFragment()
    advanceUntilIdle()

    composeTestRule.onNodeWithTag("sync list").assertIsDisplayed()
  }

  @Test
  fun `Clicking back button in toolbar navigates up`() = runWithTestDispatcher {
    var navController: NavController? = null
    fragmentScenario.launchFragmentWithNavController<SyncStatusFragment>(
      destId = R.id.sync_status_fragment,
      navControllerCallback = { navController = it },
    )
    advanceUntilIdle()

    composeTestRule.onNodeWithContentDescription("Back").performClick()
    advanceUntilIdle()

    assertThat(navController?.currentDestination?.id).isNotEqualTo(R.id.sync_status_fragment)
  }

  private suspend fun setupSurvey() {
    fakeRemoteDataStore.surveys = listOf(SURVEY)
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    surveyRepository.activateSurvey(SURVEY.id)
  }

  private fun setupFragment() {
    fragmentScenario.launchFragmentInHiltContainer<SyncStatusFragment>()
  }
}
