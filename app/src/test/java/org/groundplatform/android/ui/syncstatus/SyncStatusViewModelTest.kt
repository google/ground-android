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

package org.groundplatform.android.ui.syncstatus

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.FakeData.USER
import org.groundplatform.android.data.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.data.local.stores.LocalSubmissionStore
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.data.local.stores.LocalUserStore
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.testing.FakeDataGenerator
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SyncStatusViewModelTest : BaseHiltTest() {

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var localLoiStore: LocalLocationOfInterestStore
  @Inject lateinit var localSubmissionStore: LocalSubmissionStore
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var localUserStore: LocalUserStore
  @Inject lateinit var surveyRepository: SurveyRepositoryInterface
  @Inject lateinit var syncStatusViewModel: SyncStatusViewModel

  @Test
  fun `uiState initial value is empty`() {
    val initial = syncStatusViewModel.uiState.value
    assertThat(initial.items).isEmpty()
  }

  @Test
  fun `uiState emits empty state when no mutations in queue`() = runWithTestDispatcher {
    advanceUntilIdle()

    syncStatusViewModel.uiState.test {
      val item = awaitItem()
      assertThat(item.items).isEmpty()
    }
  }

  @Test
  fun `uiState emits LOI mutation mapped to SyncStatusDetail`() = runWithTestDispatcher {
    setupSurvey()

    localUserStore.insertOrUpdateUser(USER)
    localLoiStore.applyAndEnqueue(
      FakeDataGenerator.newLoiMutation(geometry = Point(Coordinates(0.0, 0.0)))
    )
    advanceUntilIdle()

    syncStatusViewModel.uiState.test {
      assertThat(awaitItem().items).isEmpty()

      val item = awaitItem()
      assertThat(item.items).hasSize(1)
      assertThat(item.items[0].user).isEqualTo(USER.displayName)
      assertThat(item.items[0].label).isEqualTo("Job")
      assertThat(item.items[0].subtitle).isEqualTo("Test LOI Name")
      assertThat(item.items[0].description).isEqualTo("Survey title")
      assertThat(item.items[0].status).isEqualTo(Mutation.SyncStatus.PENDING)
    }
  }

  @Test
  fun `uiState emits Submission mutation mapped to SyncStatusDetail`() = runWithTestDispatcher {
    setupSurvey()

    localUserStore.insertOrUpdateUser(USER)
    localLoiStore.apply(FakeDataGenerator.newLoiMutation(geometry = Point(Coordinates(0.0, 0.0))))
    localSubmissionStore.applyAndEnqueue(FakeDataGenerator.newSubmissionMutation())
    advanceUntilIdle()

    syncStatusViewModel.uiState.test {
      assertThat(awaitItem().items).isEmpty()

      val item = awaitItem()
      assertThat(item.items).hasSize(1)
      assertThat(item.items[0].user).isEqualTo(USER.displayName)
      assertThat(item.items[0].label).isEqualTo("Job")
      assertThat(item.items[0].subtitle).isEqualTo("Survey title")
      assertThat(item.items[0].description).isEqualTo("Test survey description")
      assertThat(item.items[0].status).isEqualTo(Mutation.SyncStatus.PENDING)
    }
  }

  private suspend fun setupSurvey() {
    fakeRemoteDataStore.surveys = listOf(SURVEY)
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    surveyRepository.activateSurvey(SURVEY.id)
  }
}
