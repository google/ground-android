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
package org.groundplatform.android.persistence.local

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFailsWith
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.FakeData.FAKE_GENERAL_ACCESS
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.User
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.model.mutation.Mutation.SyncStatus
import org.groundplatform.android.model.mutation.SubmissionMutation
import org.groundplatform.android.model.submission.Submission
import org.groundplatform.android.model.submission.SubmissionData
import org.groundplatform.android.model.submission.TextTaskData
import org.groundplatform.android.model.submission.ValueDelta
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.persistence.local.room.LocalDataStoreException
import org.groundplatform.android.persistence.local.room.dao.SubmissionDao
import org.groundplatform.android.persistence.local.room.fields.EntityDeletionState
import org.groundplatform.android.persistence.local.room.fields.MutationEntitySyncStatus
import org.groundplatform.android.persistence.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.persistence.local.stores.LocalSubmissionStore
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.local.stores.LocalUserStore
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocalSubmissionStoreTest : BaseHiltTest() {

  @Inject lateinit var localSubmissionStore: LocalSubmissionStore
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var localUserStore: LocalUserStore
  @Inject lateinit var localLoiStore: LocalLocationOfInterestStore

  @Inject lateinit var submissionDao: SubmissionDao

  @Test
  fun testApplyAndEnqueue_insertAndUpdateSubmission() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION)

    localSubmissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION)

    localSubmissionStore
      .getSubmissionMutationsByLoiIdFlow(
        TEST_SURVEY,
        TEST_LOI_MUTATION.locationOfInterestId,
        MutationEntitySyncStatus.PENDING,
      )
      .test { assertThat(expectMostRecentItem()).isEqualTo(listOf(TEST_SUBMISSION_MUTATION)) }
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, FakeData.LOI_ID)!!
    var submission = localSubmissionStore.getSubmission(loi, "submission id")
    assertEquivalent(TEST_SUBMISSION_MUTATION, submission)

    // Now update the inserted submission with new data.
    val deltas =
      listOf(
        ValueDelta(
          "task id",
          Task.Type.TEXT,
          TextTaskData.fromString("value for the really new task"),
        )
      )
    val mutation =
      TEST_SUBMISSION_MUTATION.copy(deltas = deltas, id = 2L, type = Mutation.Type.UPDATE)

    localSubmissionStore.applyAndEnqueue(mutation)

    localSubmissionStore
      .getSubmissionMutationsByLoiIdFlow(
        TEST_SURVEY,
        TEST_LOI_MUTATION.locationOfInterestId,
        MutationEntitySyncStatus.PENDING,
      )
      .test {
        assertThat(expectMostRecentItem()).isEqualTo(listOf(TEST_SUBMISSION_MUTATION, mutation))
      }

    // check if the submission was updated in the local database
    submission = localSubmissionStore.getSubmission(loi, "submission id")
    assertEquivalent(mutation, submission)

    // also test that getSubmissions returns the same submission as well
    val submissions = localSubmissionStore.getSubmissions(loi, FakeData.JOB_ID)
    assertThat(submissions).hasSize(1)
    assertEquivalent(mutation, submissions[0])
  }

  @Test
  fun testMergeSubmission() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION)
    localSubmissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION)
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, FakeData.LOI_ID)!!
    val data = SubmissionData(mapOf(Pair("task id", TextTaskData.fromString("foo value"))))
    val submission = localSubmissionStore.getSubmission(loi, "submission id").copy(data = data)
    localSubmissionStore.merge(submission)
    val mergedData = localSubmissionStore.getSubmission(loi, submission.id).data
    assertThat(mergedData.getValue("task id")).isEqualTo(TextTaskData.fromString("updated value"))
  }

  @Test
  fun testDeleteSubmission() = runWithTestDispatcher {
    // Add test submission
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION)
    localSubmissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION)
    val mutation = TEST_SUBMISSION_MUTATION.copy(id = null, type = Mutation.Type.DELETE)

    // Calling applyAndEnqueue marks the local submission as deleted.
    localSubmissionStore.applyAndEnqueue(mutation)

    // Verify that local entity exists and its state is updated.
    assertThat(submissionDao.findById("submission id")?.deletionState)
      .isEqualTo(EntityDeletionState.DELETED)

    // Verify that the local submission doesn't end up in getSubmissions().
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, FakeData.LOI_ID)!!
    assertThat(localSubmissionStore.getSubmissions(loi, "task id")).isEmpty()

    // After successful remote sync, delete submission is called by LocalMutationSyncWorker.
    localSubmissionStore.deleteSubmission("submission id")

    // Verify that the submission doesn't exist anymore
    assertFailsWith<LocalDataStoreException> {
      localSubmissionStore.getSubmission(loi, "submission id")
    }
  }

  companion object {
    private val TEST_USER = User(FakeData.USER_ID, "user@gmail.com", "user 1")
    private val TEST_TASK = Task("task id", 1, Task.Type.TEXT, "task label", false)
    private val TEST_STYLE = Style("#112233")
    private val TEST_JOB =
      Job(FakeData.JOB_ID, TEST_STYLE, "heading title", mapOf(Pair(TEST_TASK.id, TEST_TASK)))
    private val TEST_SURVEY =
      Survey(
        FakeData.SURVEY_ID,
        "survey 1",
        "foo description",
        mapOf(Pair(TEST_JOB.id, TEST_JOB)),
        generalAccess = FAKE_GENERAL_ACCESS,
      )
    private val TEST_POINT = Point(Coordinates(110.0, -23.1))
    private val TEST_LOI_MUTATION = FakeData.newLoiMutation(TEST_POINT)
    private val TEST_SUBMISSION_MUTATION =
      SubmissionMutation(
        job = TEST_JOB,
        submissionId = "submission id",
        deltas =
          listOf(ValueDelta("task id", Task.Type.TEXT, TextTaskData.fromString("updated value"))),
        id = 1L,
        type = Mutation.Type.CREATE,
        syncStatus = SyncStatus.PENDING,
        surveyId = FakeData.SURVEY_ID,
        locationOfInterestId = FakeData.LOI_ID,
        userId = FakeData.USER_ID,
        collectionId = "",
      )

    private fun assertEquivalent(mutation: SubmissionMutation, submission: Submission) {
      assertThat(mutation.submissionId).isEqualTo(submission.id)
      assertThat(mutation.locationOfInterestId).isEqualTo(submission.locationOfInterest.id)
      assertThat(mutation.job).isEqualTo(submission.job)
      assertThat(mutation.surveyId).isEqualTo(submission.surveyId)
      assertThat(mutation.userId).isEqualTo(submission.lastModified.user.id)
      assertThat(mutation.userId).isEqualTo(submission.created.user.id)
      MatcherAssert.assertThat(
        SubmissionData().copyWithDeltas(mutation.deltas),
        Matchers.samePropertyValuesAs(submission.data),
      )
    }
  }
}
