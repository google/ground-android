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
package com.google.android.ground.persistence.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result.retry
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus.*
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestStore
import com.google.android.ground.persistence.local.stores.LocalSubmissionStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.local.stores.LocalUserStore
import com.google.android.ground.persistence.sync.LocalMutationSyncWorker.Companion.LOCATION_OF_INTEREST_ID_PARAM_KEY
import com.google.android.ground.repository.MutationRepository
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.sharedtest.FakeData
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocalMutationSyncWorkerTest : BaseHiltTest() {

  private lateinit var context: Context

  @Mock private lateinit var mockMediaUploadWorkManager: MediaUploadWorkManager

  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  @Inject lateinit var localLocationOfInterestStore: LocalLocationOfInterestStore

  @Inject lateinit var localSubmissionStore: LocalSubmissionStore

  @Inject lateinit var localSurveyStore: LocalSurveyStore

  @Inject lateinit var localUserStore: LocalUserStore

  @Inject lateinit var mutationRepository: MutationRepository

  private val factory =
    object : WorkerFactory() {
      override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
      ): ListenableWorker =
        LocalMutationSyncWorker(
          appContext,
          workerParameters,
          mutationRepository,
          localUserStore,
          fakeRemoteDataStore,
          mockMediaUploadWorkManager,
        )
    }

  @Before
  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
    runBlocking {
      localUserStore.insertOrUpdateUser(FakeData.USER.copy(id = TEST_USER_ID))
      localSurveyStore.insertOrUpdateSurvey(FakeData.SURVEY.copy(id = TEST_SURVEY_ID))
    }
  }

  @Test
  fun `Throws an NPE if LOI ID is null`() {
    assertThrows(NullPointerException::class.java) {
      runWithTestDispatcher { createAndDoWork(context, null) }
    }
  }

  @Test
  fun `Succeeds if there are 0 pending mutations`() = runWithTestDispatcher {
    val result = createAndDoWork(context, TEST_LOI_ID)

    assertThat(result).isEqualTo(success())
  }

  @Test
  fun `Succeeds if there are non-zero pending mutations`() = runWithTestDispatcher {
    addPendingMutations()

    val result = createAndDoWork(context, TEST_LOI_ID)

    assertThat(result).isEqualTo(success())
    assertMutationsState(complete = 2)
  }

  @Test
  fun `Retries if there are non-zero pending mutations but remote sync fails`() =
    runWithTestDispatcher {
      fakeRemoteDataStore.applyMutationError = Error(ERROR_MESSAGE)
      addPendingMutations()

      val result = createAndDoWork(context, TEST_LOI_ID)

      assertThat(result).isEqualTo(retry())
      assertMutationsState(
        failed = 2,
        retryCount = listOf(1, 1),
        lastErrors = listOf(ERROR_MESSAGE, ERROR_MESSAGE),
      )
    }

  @Test
  fun `Worker retries on failure`() = runWithTestDispatcher {
    fakeRemoteDataStore.applyMutationError = Error(ERROR_MESSAGE)
    addPendingMutations()

    var result = createAndDoWork(context, TEST_LOI_ID)
    assertThat(result).isEqualTo(retry())
    assertMutationsState(
      failed = 2,
      retryCount = listOf(1, 1),
      lastErrors = listOf(ERROR_MESSAGE, ERROR_MESSAGE),
    )

    for (i in 1..10) {
      // Worker should retry N times.
      result = createAndDoWork(context, TEST_LOI_ID)
      assertThat(result).isEqualTo(retry())
      // Verify that the retryCount has incremented.
      assertMutationsState(
        failed = 2,
        retryCount = listOf(i + 1, i + 1),
        lastErrors = listOf(ERROR_MESSAGE, ERROR_MESSAGE),
      )
    }
  }

  private suspend fun assertMutationsState(
    pending: Int = 0,
    inProgress: Int = 0,
    complete: Int = 0,
    failed: Int = 0,
    retryCount: List<Int> = listOf(),
    lastErrors: List<String> = listOf(),
  ) {
    assertWithMessage("Unknown mutations count incorrect")
      .that(mutationRepository.getMutations(TEST_LOI_ID, UNKNOWN))
      .hasSize(0)
    assertWithMessage("Pending mutations count incorrect")
      .that(mutationRepository.getMutations(TEST_LOI_ID, PENDING))
      .hasSize(pending)
    assertWithMessage("In Progress mutations count incorrect")
      .that(mutationRepository.getMutations(TEST_LOI_ID, IN_PROGRESS))
      .hasSize(inProgress)
    assertWithMessage("Completed mutations count incorrect")
      .that(mutationRepository.getMutations(TEST_LOI_ID, MEDIA_UPLOAD_PENDING))
      .hasSize(complete)

    val failedMutations = mutationRepository.getMutations(TEST_LOI_ID, FAILED)
    assertWithMessage("Failed mutations count incorrect").that(failedMutations).hasSize(failed)
    assertThat(failedMutations.map { it.retryCount.toInt() }).containsExactlyElementsIn(retryCount)
    assertThat(failedMutations.map { it.lastError }).containsExactlyElementsIn(lastErrors)
  }

  private suspend fun addPendingMutations() {
    localLocationOfInterestStore.applyAndEnqueue(createLoiMutation())
    localSubmissionStore.applyAndEnqueue(createSubmissionMutation())
  }

  private fun createLoiMutation() =
    LocationOfInterestMutation(
      type = Mutation.Type.CREATE,
      syncStatus = Mutation.SyncStatus.PENDING,
      locationOfInterestId = TEST_LOI_ID,
      userId = TEST_USER_ID,
      surveyId = TEST_SURVEY_ID,
      geometry = TEST_GEOMETRY,
    )

  private fun createSubmissionMutation() =
    SubmissionMutation(
      type = Mutation.Type.CREATE,
      syncStatus = Mutation.SyncStatus.PENDING,
      locationOfInterestId = TEST_LOI_ID,
      userId = TEST_USER_ID,
      job = TEST_JOB,
      surveyId = TEST_SURVEY_ID,
    )

  private suspend fun createAndDoWork(context: Context, loiId: String?): ListenableWorker.Result =
    TestListenableWorkerBuilder<LocalMutationSyncWorker>(
        context,
        inputData = workDataOf(Pair(LOCATION_OF_INTEREST_ID_PARAM_KEY, loiId)),
      )
      .setWorkerFactory(factory)
      .build()
      .doWork()

  companion object {
    private const val ERROR_MESSAGE = "network unavailable"
    private const val TEST_LOI_ID = "loiId"
    private const val TEST_SURVEY_ID = "surveyId"
    private const val TEST_USER_ID = "userId"
    private val TEST_GEOMETRY = Point(FakeData.COORDINATES)
    private val TEST_JOB = FakeData.JOB
  }
}
