/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.domain.usecase.loi

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.util.Date
import javax.inject.Inject
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.data.repository.RepositoryModule
import org.groundplatform.android.domain.repository.LocationOfInterestRepository
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.mutation.LocationOfInterestMutation
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.persistence.sync.MutationSyncWorkManager
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@UninstallModules(RepositoryModule::class)
class SaveLoiUseCaseTest : BaseHiltTest() {
  @BindValue @Mock lateinit var mockLoiRepository: LocationOfInterestRepository
  @BindValue @Mock lateinit var mockWorkManager: MutationSyncWorkManager

  @Inject lateinit var saveLoiUseCase: SaveLoiUseCase

  @Test
  fun testApplyAndEnqueue_enqueuesWorker() = runWithTestDispatcher {
    saveLoi()

    argumentCaptor<LocationOfInterestMutation> {
      verify(mockLoiRepository).applyAndEnqueue(capture())

      val date = Date()
      val mutation = firstValue.copy(clientTimestamp = date) // replace timestamp
      assertThat(mutation)
        .isEqualTo(
          LocationOfInterestMutation(
            jobId = "job id",
            type = Mutation.Type.CREATE,
            syncStatus = Mutation.SyncStatus.PENDING,
            surveyId = "survey id",
            locationOfInterestId = "TEST UUID",
            userId = "user id",
            geometry = POINT_GEOMETRY,
            collectionId = "loiId",
            properties = mapOf(),
            isPredefined = false,
            clientTimestamp = date,
          )
        )
    }

    verify(mockWorkManager).enqueueSyncWorker()
  }

  @Test
  fun testApplyAndEnqueue_returnsErrorOnWorkerSyncFailure() = runWithTestDispatcher {
    whenever(mockWorkManager.enqueueSyncWorker()).thenThrow(Error())

    assertFailsWith<Error> { saveLoi() }

    verify(mockWorkManager, times(1)).enqueueSyncWorker()
  }

  private suspend fun saveLoi() {
    saveLoiUseCase(
      collectionId = "loiId",
      geometry = POINT_GEOMETRY,
      job = TEST_SURVEY.jobs.first(),
      loiName = null,
      surveyId = TEST_SURVEY.id,
    )
  }

  // TODO: Add tests for new LOI sync once implemented (create, update, delete, error).
  // Issue URL: https://github.com/google/ground-android/issues/1373

  // TODO: Add tests for getLocationsOfInterest once new LOI sync implemented.
  // Issue URL: https://github.com/google/ground-android/issues/1373

  companion object {
    private val COORDINATE = Coordinates(-20.0, -20.0)
    private val POINT_GEOMETRY = Point(COORDINATE)
    private val TEST_SURVEY = FakeData.SURVEY
  }
}
