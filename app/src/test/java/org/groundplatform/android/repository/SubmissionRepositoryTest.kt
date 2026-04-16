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
package org.groundplatform.android.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.local.stores.LocalSubmissionStore
import org.groundplatform.android.data.sync.MutationSyncWorkManager
import org.groundplatform.android.data.uuid.OfflineUuidGenerator
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.locationofinterest.AuditInfo
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.domain.model.mutation.SubmissionMutation
import org.groundplatform.domain.model.submission.DraftSubmission
import org.groundplatform.domain.model.submission.TextTaskData
import org.groundplatform.domain.model.submission.ValueDelta
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.groundplatform.testcommon.FakeDataGenerator
import org.groundplatform.testcommon.Task
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SubmissionRepositoryTest {

  @Mock private lateinit var localSubmissionStore: LocalSubmissionStore
  @Mock private lateinit var localValueStore: LocalValueStore
  @Mock private lateinit var locationOfInterestRepository: LocationOfInterestRepositoryInterface
  @Mock private lateinit var mutationSyncWorkManager: MutationSyncWorkManager
  @Mock private lateinit var userRepository: UserRepositoryInterface
  @Mock private lateinit var uuidGenerator: OfflineUuidGenerator

  private lateinit var repository: SubmissionRepository

  @Before
  fun setUp() {
    repository =
      SubmissionRepository(
        localSubmissionStore,
        localValueStore,
        locationOfInterestRepository,
        mutationSyncWorkManager,
        userRepository,
        uuidGenerator,
      )
  }

  @Test
  fun `saveSubmission creates a submission mutation and enqueues sync`() = runTest {
    setupMocks()

    repository.saveSubmission(
      surveyId = TEST_SURVEY.id,
      locationOfInterestId = TEST_LOI.id,
      deltas = TEST_DELTAS,
      collectionId = TEST_COLLECTION_ID,
    )

    val captor = argumentCaptor<SubmissionMutation>()
    verify(localSubmissionStore).applyAndEnqueue(captor.capture())
    with(captor.firstValue) {
      assertThat(job).isEqualTo(TEST_JOB)
      assertThat(submissionId).isEqualTo(TEST_UUID)
      assertThat(type).isEqualTo(Mutation.Type.CREATE)
      assertThat(deltas).isEqualTo(deltas)
      assertThat(syncStatus).isEqualTo(Mutation.SyncStatus.PENDING)
      assertThat(surveyId).isEqualTo(TEST_SURVEY.id)
      assertThat(locationOfInterestId).isEqualTo(TEST_LOI.id)
      assertThat(userId).isEqualTo(TEST_USER.id)
      assertThat(collectionId).isEqualTo(collectionId)
    }
    verify(mutationSyncWorkManager).enqueueSyncWorker()
  }

  @Test
  fun `saveSubmission does nothing when LOI not found`() = runTest {
    setupMocks(loi = null)

    repository.saveSubmission(
      surveyId = TEST_SURVEY.id,
      locationOfInterestId = TEST_LOI.id,
      deltas = TEST_DELTAS,
      collectionId = TEST_COLLECTION_ID,
    )

    verify(localSubmissionStore, never()).applyAndEnqueue(any())
    verify(mutationSyncWorkManager, never()).enqueueSyncWorker()
  }

  @Test
  fun `getDraftSubmission gets the draft from the local store`() = runTest {
    setupMocks()
    val result = repository.getDraftSubmission(DRAFT_SUBMISSION.id, TEST_SURVEY)

    assertThat(result).isEqualTo(DRAFT_SUBMISSION)
  }

  @Test
  fun `getDraftSubmission returns null when not found`() = runTest {
    setupMocks(draftSubmissions = null)

    assertThat(repository.getDraftSubmission("missing", TEST_SURVEY)).isNull()
  }

  @Test
  fun `countDraftSubmissions counts the draft submissions in the local store`() = runTest {
    setupMocks(
      draftSubmissions =
        listOf(
          DRAFT_SUBMISSION,
          DRAFT_SUBMISSION.copy(id = "draft-2", surveyId = "survey2"),
          DRAFT_SUBMISSION.copy(id = "draft-3", surveyId = "survey3"),
        )
    )

    assertThat(repository.countDraftSubmissions()).isEqualTo(3)
  }

  @Test
  fun `getDraftSubmissionsId returns id from local value store`() = runTest {
    val selectedDraftId = "draft-3"
    setupMocks(
      draftSubmissions =
        listOf(
          DRAFT_SUBMISSION,
          DRAFT_SUBMISSION.copy(id = "draft-2", surveyId = "survey2"),
          DRAFT_SUBMISSION.copy(id = "draft-3", surveyId = "survey3"),
        ),
      selectedDraftId = selectedDraftId,
    )

    assertThat(repository.getDraftSubmissionsId()).isEqualTo(selectedDraftId)
  }

  @Test
  fun `getDraftSubmissionsId returns empty string when there is no draftSubmissionId stored`() =
    runTest {
      setupMocks(draftSubmissions = listOf(DRAFT_SUBMISSION), selectedDraftId = null)

      assertThat(repository.getDraftSubmissionsId()).isEmpty()
    }

  @Test
  fun `saveDraftSubmission saves and updates the id in the local store`() = runTest {
    setupMocks()

    repository.saveDraftSubmission(
      TEST_JOB.id,
      TEST_LOI.id,
      TEST_SURVEY.id,
      TEST_DELTAS,
      null,
      TEST_CURRENT_TASK_ID,
    )

    val captor = argumentCaptor<DraftSubmission>()
    verify(localSubmissionStore).saveDraftSubmission(draftSubmission = captor.capture())
    with(captor.firstValue) {
      assertThat(id).isEqualTo(TEST_UUID)
      assertThat(jobId).isEqualTo(TEST_JOB.id)
      assertThat(loiId).isEqualTo(TEST_LOI.id)
      assertThat(loiName).isEqualTo(null)
      assertThat(surveyId).isEqualTo(TEST_SURVEY.id)
      assertThat(currentTaskId).isEqualTo(TEST_CURRENT_TASK_ID)
    }
    verify(localValueStore).draftSubmissionId = TEST_UUID
  }

  @Test
  fun `deleteDraftSubmission clears draft and id from the local store`() = runTest {
    setupMocks()

    repository.deleteDraftSubmission()

    verify(localSubmissionStore).deleteDraftSubmissions()
    verify(localValueStore).draftSubmissionId = null
  }

  @Test
  fun `getTotalSubmissionCount accounts for pending creates and deletes`() = runTest {
    val loi = TEST_LOI.copy(submissionCount = 10)
    setupMocks(pendingCreateCount = 3, pendingDeleteCount = 1, loi = loi)

    assertThat(repository.getTotalSubmissionCount(loi)).isEqualTo(12)
  }

  @Test
  fun `getPendingCreateCount gets the count from the local store`() = runTest {
    val loi = TEST_LOI.copy(submissionCount = 7)
    setupMocks(pendingCreateCount = 7, loi = loi)

    assertThat(repository.getPendingCreateCount(loi.id)).isEqualTo(7)
  }

  private suspend fun setupMocks(
    uuid: String = TEST_UUID,
    loi: LocationOfInterest? = TEST_LOI,
    authenticatedUser: User = TEST_USER,
    draftSubmissions: List<DraftSubmission>? = listOf(DRAFT_SUBMISSION),
    selectedDraftId: String? = DRAFT_SUBMISSION.id,
    pendingCreateCount: Int = 0,
    pendingDeleteCount: Int = 0,
  ) {
    whenever(uuidGenerator.generateUuid()).thenReturn(uuid)
    whenever(userRepository.getAuthenticatedUser()).thenReturn(authenticatedUser)
    whenever(locationOfInterestRepository.getOfflineLoi(any(), any())).thenReturn(loi)
    whenever(localSubmissionStore.getDraftSubmission(any(), any()))
      .thenReturn(draftSubmissions?.firstOrNull { it.id == selectedDraftId })
    whenever(localSubmissionStore.countDraftSubmissions()).thenReturn(draftSubmissions?.size)
    whenever(localValueStore.draftSubmissionId).thenReturn(selectedDraftId)
    whenever(localSubmissionStore.getPendingCreateCount(any())).thenReturn(pendingCreateCount)
    whenever(localSubmissionStore.getPendingDeleteCount(any())).thenReturn(pendingDeleteCount)
  }

  private companion object {
    const val TEST_UUID = "test-uuid"
    const val TEST_CURRENT_TASK_ID = "currentTaskId"
    const val TEST_COLLECTION_ID = "collection1"
    val TEST_DELTAS = listOf(ValueDelta("taskId", Task.Type.TEXT, TextTaskData("textTaskResult")))
    val TEST_USER = FakeDataGenerator.newUser()
    val TEST_JOB = FakeDataGenerator.newJob()
    val TEST_SURVEY =
      FakeDataGenerator.newSurvey(
        jobMap = mapOf(TEST_JOB.id to TEST_JOB),
        acl = mapOf(TEST_USER.email to "DATA_COLLECTOR"),
      )
    val TEST_LOI =
      FakeDataGenerator.newLocationOfInterest(
        surveyId = TEST_SURVEY.id,
        job = TEST_JOB,
        created = AuditInfo(TEST_USER),
        lastModified = AuditInfo(TEST_USER),
      )
    val DRAFT_SUBMISSION =
      FakeDataGenerator.newDraftSubmission(
        jobId = TEST_JOB.id,
        loiId = TEST_LOI.id,
        surveyId = TEST_SURVEY.id,
        deltas = TEST_DELTAS,
        currentTaskId = TEST_CURRENT_TASK_ID,
      )
  }
}
