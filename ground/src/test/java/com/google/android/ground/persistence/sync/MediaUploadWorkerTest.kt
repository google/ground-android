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
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.PhotoTaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.stores.LocalLocationOfInterestStore
import com.google.android.ground.persistence.local.stores.LocalSubmissionStore
import com.google.android.ground.persistence.local.stores.LocalSurveyStore
import com.google.android.ground.persistence.local.stores.LocalUserStore
import com.google.android.ground.repository.MutationRepository
import com.google.android.ground.repository.UserMediaRepository
import com.google.common.truth.Truth.assertThat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sharedtest.FakeData
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import com.sharedtest.persistence.remote.FakeRemoteStorageManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MediaUploadWorkerTest : BaseHiltTest() {
  private lateinit var context: Context
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var mutationRepository: MutationRepository
  @Inject lateinit var userMediaRepository: UserMediaRepository
  @Inject lateinit var fakeRemoteStorageManager: FakeRemoteStorageManager
  @Inject lateinit var localSubmissionStore: LocalSubmissionStore
  @Inject lateinit var localUserStore: LocalUserStore
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var localLocationOfInterestStore: LocalLocationOfInterestStore
  @BindValue @Mock lateinit var mockFirebaseCrashlytics: FirebaseCrashlytics

  private val factory =
    object : WorkerFactory() {
      override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
      ): ListenableWorker =
        MediaUploadWorker(
          context,
          workerParameters,
          fakeRemoteStorageManager,
          mutationRepository,
          userMediaRepository,
        )
    }

  @Before
  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun doWork_throwsOnEmptyLoiId() {
    assertThrows(IllegalStateException::class.java) {
      runWithTestDispatcher { createAndDoWork(context, "") }
    }
  }

  @Test
  fun doWork_succeedsOnExistingPhoto() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(FakeData.USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLocationOfInterestStore.insertOrUpdate(TEST_LOI)
    localSubmissionStore.applyAndEnqueue(
      createSubmissionMutation().copy(syncStatus = Mutation.SyncStatus.MEDIA_UPLOAD_PENDING)
    )
    createAndDoWork(context, FakeData.LOCATION_OF_INTEREST.id)
    assertThatMutationCountEquals(MutationEntitySyncStatus.COMPLETED, 1)
  }

  @Test
  fun doWork_failsOnNonExistentPhotos() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(FakeData.USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLocationOfInterestStore.insertOrUpdate(TEST_LOI)
    localSubmissionStore.applyAndEnqueue(
      createSubmissionMutation("does_not_exist.jpg")
        .copy(syncStatus = Mutation.SyncStatus.MEDIA_UPLOAD_PENDING)
    )
    createAndDoWork(context, FakeData.LOCATION_OF_INTEREST.id)
    assertThatMutationCountEquals(MutationEntitySyncStatus.FAILED, 1)
  }

  @Test
  fun doWork_propagatesFailures() = runWithTestDispatcher {
    val mutation = createSubmissionMutation() // a valid mutation
    val delta = buildList {
      addAll(mutation.deltas)
      add(ValueDelta(PHOTO_TASK_ID, Task.Type.PHOTO, PhotoTaskData("some/path/does_not_exist.jpg")))
    }
    val updatedMutation =
      mutation.copy(
        deltas = delta,
        syncStatus = Mutation.SyncStatus.MEDIA_UPLOAD_PENDING,
      ) // add an additional non-existent photo to the mutation

    localUserStore.insertOrUpdateUser(FakeData.USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLocationOfInterestStore.insertOrUpdate(TEST_LOI)
    localSubmissionStore.applyAndEnqueue(updatedMutation)

    createAndDoWork(context, FakeData.LOCATION_OF_INTEREST.id)
    assertThatMutationCountEquals(MutationEntitySyncStatus.FAILED, 1)
    assertThatMutationCountEquals(MutationEntitySyncStatus.MEDIA_UPLOAD_PENDING, 0)
    assertThatMutationCountEquals(MutationEntitySyncStatus.MEDIA_UPLOAD_IN_PROGRESS, 0)
    assertThatMutationCountEquals(MutationEntitySyncStatus.COMPLETED, 0)
  }

  @Test
  fun doWork_ignoresNonMediaMutations() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(FakeData.USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLocationOfInterestStore.insertOrUpdate(TEST_LOI)
    addSubmissionMutationToLocalStorage(Mutation.SyncStatus.PENDING)
    addSubmissionMutationToLocalStorage(Mutation.SyncStatus.FAILED)
    addSubmissionMutationToLocalStorage(Mutation.SyncStatus.IN_PROGRESS)
    addSubmissionMutationToLocalStorage(Mutation.SyncStatus.COMPLETED)
    addSubmissionMutationToLocalStorage(Mutation.SyncStatus.UNKNOWN)

    createAndDoWork(context, FakeData.LOCATION_OF_INTEREST.id)
    assertThatMutationCountEquals(MutationEntitySyncStatus.FAILED, 1)
    assertThatMutationCountEquals(MutationEntitySyncStatus.PENDING, 1)
    assertThatMutationCountEquals(MutationEntitySyncStatus.COMPLETED, 1)
    assertThatMutationCountEquals(MutationEntitySyncStatus.IN_PROGRESS, 1)
    assertThatMutationCountEquals(MutationEntitySyncStatus.UNKNOWN, 1)
    assertThatMutationCountEquals(MutationEntitySyncStatus.MEDIA_UPLOAD_PENDING, 0)
    assertThatMutationCountEquals(MutationEntitySyncStatus.MEDIA_UPLOAD_IN_PROGRESS, 0)
  }

  // Initiates and runs the MediaUploadWorker
  private suspend fun createAndDoWork(context: Context, loiId: String) {
    TestListenableWorkerBuilder<MediaUploadWorker>(
        context,
        MediaUploadWorker.createInputData(loiId),
      )
      .setWorkerFactory(factory)
      .build()
      .doWork()
  }

  // Assert a given number of mutations of the specified status exist.
  private suspend fun assertThatMutationCountEquals(status: MutationEntitySyncStatus, count: Int) {
    assertThat(mutationRepository.getMutations(FakeData.LOCATION_OF_INTEREST.id, status))
      .hasSize(count)
  }

  private suspend fun addSubmissionMutationToLocalStorage(status: Mutation.SyncStatus) =
    localSubmissionStore.applyAndEnqueue(createSubmissionMutation().copy(syncStatus = status))

  private fun createSubmissionMutation(photoName: String? = null): SubmissionMutation {
    val photo =
      userMediaRepository.savePhoto(
        Bitmap.createBitmap(1, 1, Bitmap.Config.HARDWARE),
        TEST_PHOTO_TASK.id,
      )

    return SUBMISSION_MUTATION.copy(
      job = TEST_JOB,
      deltas =
        listOf(ValueDelta(PHOTO_TASK_ID, Task.Type.PHOTO, PhotoTaskData(photoName ?: photo.name))),
    )
  }

  // TODO: Replace all this w/ FakeData functions that return good base model objects.
  companion object {
    private const val PHOTO_TASK_ID = "photo_task_id"
    private val TEST_PHOTO_TASK: Task =
      Task(
        id = "photo_task_id",
        index = 1,
        isRequired = true,
        type = Task.Type.PHOTO,
        label = "photo_task",
      )

    private val TEST_JOB = FakeData.JOB.copy(tasks = mapOf(PHOTO_TASK_ID to TEST_PHOTO_TASK))

    private val TEST_LOI = FakeData.LOCATION_OF_INTEREST.copy(job = TEST_JOB)
    private val TEST_SURVEY = FakeData.SURVEY.copy(jobMap = mapOf(TEST_JOB.id to TEST_JOB))

    val SUBMISSION_MUTATION =
      SubmissionMutation(
        type = Mutation.Type.CREATE,
        syncStatus = Mutation.SyncStatus.PENDING,
        locationOfInterestId = FakeData.LOCATION_OF_INTEREST.id,
        userId = FakeData.USER.id,
        job = FakeData.JOB,
        surveyId = FakeData.SURVEY.id,
        collectionId = "collectionId",
        deltas =
          listOf(
            ValueDelta(PHOTO_TASK_ID, Task.Type.PHOTO, PhotoTaskData("foo/$PHOTO_TASK_ID.jpg"))
          ),
      )
  }
}
