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

package org.groundplatform.data.repository

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.groundplatform.data.FakeLocalSubmissionStore
import org.groundplatform.data.FakeLocalValueStore
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.domain.model.submission.DraftSubmission
import org.groundplatform.domain.model.submission.ValueDelta
import org.groundplatform.domain.model.task.Task
import org.groundplatform.testing.FakeDataGenerator
import org.groundplatform.testing.FakeLocationOfInterestRepository
import org.groundplatform.testing.FakeMutationSyncManager
import org.groundplatform.testing.FakeOfflineUuidGenerator
import org.groundplatform.testing.FakeUserRepository

class SubmissionRepositoryTest {

  private val fakeSubmissionStore = FakeLocalSubmissionStore()
  private val fakeValueStore = FakeLocalValueStore()
  private val fakeLoiRepository = FakeLocationOfInterestRepository()
  private val fakeMutationSyncManager = FakeMutationSyncManager()
  private val fakeUserRepository = FakeUserRepository()
  private val fakeUuidGenerator = FakeOfflineUuidGenerator("test-uuid")

  private lateinit var submissionRepository: SubmissionRepository

  @BeforeTest
  fun setUp() {
    submissionRepository =
      SubmissionRepository(
        localSubmissionStore = fakeSubmissionStore,
        localValueStore = fakeValueStore,
        locationOfInterestRepository = fakeLoiRepository,
        mutationSyncManager = fakeMutationSyncManager,
        userRepository = fakeUserRepository,
        uuidGenerator = fakeUuidGenerator,
      )
  }

  @Test
  fun saveSubmission_createsMutationAndEnqueuesSync() = runTest {
    val loi = FakeDataGenerator.newLocationOfInterest(job = Job(id = "job1"))
    fakeLoiRepository.offlineLoi = loi

    submissionRepository.saveSubmission(
      surveyId = "s1",
      locationOfInterestId = loi.id,
      deltas = listOf(ValueDelta("task1", Task.Type.TEXT, null)),
      collectionId = "c1",
    )

    assertEquals(1, fakeSubmissionStore.appliedMutations.size)
    val mutation = fakeSubmissionStore.appliedMutations.first()
    assertEquals("test-uuid", mutation.submissionId)
    assertEquals(Mutation.Type.CREATE, mutation.type)
    assertEquals(1, fakeMutationSyncManager.enqueueSyncCount)
  }

  @Test
  fun getDraftSubmission_returnsDraftWhenSurveyMatches() = runTest {
    val survey =
      Survey(
        id = "s1",
        title = "Survey 1",
        description = "Description",
        jobMap = emptyMap(),
        generalAccess = Survey.GeneralAccess.PUBLIC,
      )
    val draft =
      DraftSubmission(
        id = "draft1",
        jobId = "job1",
        loiId = "loi1",
        loiName = "LOI 1",
        surveyId = "s1",
        deltas = emptyList(),
        currentTaskId = "task1",
      )
    fakeValueStore.draftSubmissionId = "draft1"
    fakeSubmissionStore.saveDraftSubmission(draft)

    val result = submissionRepository.getDraftSubmission(survey)

    assertEquals(draft, result)
  }

  @Test
  fun getDraftSubmission_returnsNullWhenSurveyMismatch() = runTest {
    val survey1 =
      Survey(
        id = "s1",
        title = "Survey 1",
        description = "Description",
        jobMap = emptyMap(),
        generalAccess = Survey.GeneralAccess.PUBLIC,
      )
    val draft =
      DraftSubmission(
        id = "draft1",
        jobId = "job1",
        loiId = "loi1",
        loiName = "LOI 1",
        surveyId = "other_survey",
        deltas = emptyList(),
        currentTaskId = "task1",
      )
    fakeValueStore.draftSubmissionId = "draft1"
    fakeSubmissionStore.saveDraftSubmission(draft)

    val result = submissionRepository.getDraftSubmission(survey1)

    assertNull(result)
  }

  @Test
  fun saveDraftSubmission_savesDraftAndSetsIdInValueStore() = runTest {
    fakeUuidGenerator.nextUuid = "draft-123"

    submissionRepository.saveDraftSubmission(
      jobId = "job1",
      loiId = "loi1",
      surveyId = "s1",
      deltas = emptyList(),
      loiName = "Test LOI",
      currentTaskId = "task1",
    )

    assertEquals("draft-123", fakeValueStore.draftSubmissionId)
    assertEquals(1, fakeSubmissionStore.countDraftSubmissions())
  }

  @Test
  fun deleteDraftSubmission_deletesFromStoreAndClearsValueStore() = runTest {
    fakeValueStore.draftSubmissionId = "draft-123"
    fakeSubmissionStore.saveDraftSubmission(
      DraftSubmission(
        id = "draft-123",
        jobId = "job1",
        loiId = "loi1",
        loiName = "LOI",
        surveyId = "s1",
        deltas = emptyList(),
        currentTaskId = "task1",
      )
    )

    submissionRepository.deleteDraftSubmission()

    assertNull(fakeValueStore.draftSubmissionId)
    assertEquals(0, fakeSubmissionStore.countDraftSubmissions())
  }

  @Test
  fun getTotalSubmissionCount_calculatesCountCorrectly() = runTest {
    val loi =
      FakeDataGenerator.newLocationOfInterest(id = "loi1", job = Job(id = "job1"))
        .copy(submissionCount = 5)
    fakeSubmissionStore.pendingCreateCount = 3
    fakeSubmissionStore.pendingDeleteCount = 1

    val total = submissionRepository.getTotalSubmissionCount(loi)

    assertEquals(7, total)
  }
}
