/*
 * Copyright 2020 Google LLC
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.User
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.imagery.OfflineArea
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
import org.groundplatform.android.persistence.local.room.converter.formatVertices
import org.groundplatform.android.persistence.local.room.converter.parseVertices
import org.groundplatform.android.persistence.local.room.dao.LocationOfInterestDao
import org.groundplatform.android.persistence.local.room.dao.SubmissionDao
import org.groundplatform.android.persistence.local.room.fields.EntityDeletionState
import org.groundplatform.android.persistence.local.room.fields.MutationEntitySyncStatus
import org.groundplatform.android.persistence.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.persistence.local.stores.LocalOfflineAreaStore
import org.groundplatform.android.persistence.local.stores.LocalSubmissionStore
import org.groundplatform.android.persistence.local.stores.LocalSurveyStore
import org.groundplatform.android.persistence.local.stores.LocalUserStore
import org.groundplatform.android.ui.map.Bounds
import org.groundplatform.android.ui.map.gms.GmsExt.getShellCoordinates
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocalDataStoreTests : BaseHiltTest() {
  // TODO: Split into multiple test suites, one for each SoT.
  // Issue URL: https://github.com/google/ground-android/issues/1491
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var localUserStore: LocalUserStore
  @Inject lateinit var localSubmissionStore: LocalSubmissionStore
  @Inject lateinit var localLoiStore: LocalLocationOfInterestStore
  @Inject lateinit var localOfflineAreaStore: LocalOfflineAreaStore
  @Inject lateinit var localValueStore: LocalValueStore
  // TODO: Use public interface of data stores instead of inspecting state of impl (DAOs).
  // Issue URL: https://github.com/google/ground-android/issues/1470
  @Inject lateinit var submissionDao: SubmissionDao
  @Inject lateinit var locationOfInterestDao: LocationOfInterestDao

  @Test
  fun testInsertAndGetSurveys() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    assertThat(localSurveyStore.surveys.first()).containsExactly(TEST_SURVEY)
  }

  @Test
  fun testGetSurveyById() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    assertThat(localSurveyStore.getSurveyById(TEST_SURVEY.id)).isEqualTo(TEST_SURVEY)
  }

  @Test
  fun testDeleteSurvey() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localSurveyStore.deleteSurvey(TEST_SURVEY)
    assertThat(localSurveyStore.surveys.first()).isEmpty()
  }

  @Test
  fun testRemovedJobFromSurvey() = runWithTestDispatcher {
    val job1 = Job("job 1", TEST_STYLE, "job 1 name")
    val job2 = Job("job 2", TEST_STYLE, "job 2 name")
    var survey =
      Survey("foo id", "foo survey", "foo survey description", mapOf(Pair(job1.id, job1)))
    localSurveyStore.insertOrUpdateSurvey(survey)
    survey = Survey("foo id", "foo survey", "foo survey description", mapOf(Pair(job2.id, job2)))
    localSurveyStore.insertOrUpdateSurvey(survey)
    val updatedSurvey = localSurveyStore.getSurveyById("foo id")
    assertThat(updatedSurvey?.jobs).hasSize(1)
    assertThat(updatedSurvey?.jobs?.first()).isEqualTo(job2)
  }

  @Test
  fun testInsertAndGetUser() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    assertThat(localUserStore.getUser(FakeData.USER_ID)).isEqualTo(TEST_USER)
  }

  @Test
  fun testApplyAndEnqueue_insertsLoi() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)

    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION)

    assertThat(
        localLoiStore
          .getLocationOfInterest(TEST_SURVEY, TEST_LOI_MUTATION.locationOfInterestId)
          ?.geometry
      )
      .isEqualTo(TEST_POINT)
  }

  @Test
  fun testApplyAndEnqueue_insertsMutation() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)

    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION)
    advanceUntilIdle()

    localLoiStore.getAllSurveyMutations(TEST_SURVEY).test {
      assertThat(expectMostRecentItem()).isEqualTo(listOf(TEST_LOI_MUTATION))
    }
  }

  @Test
  fun testApplyAndEnqueue_insertPolygonLoi() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)

    localLoiStore.applyAndEnqueue(TEST_POLYGON_LOI_MUTATION)

    assertThat(
        localLoiStore
          .getLocationOfInterest(TEST_SURVEY, TEST_POLYGON_LOI_MUTATION.locationOfInterestId)
          ?.geometry
      )
      .isEqualTo(TEST_POLYGON_LOI_MUTATION.geometry)
  }

  @Test
  fun testApplyAndEnqueue_enqueuesPolygonLoiMutation() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLoiStore.applyAndEnqueue(TEST_POLYGON_LOI_MUTATION)

    localLoiStore.getAllSurveyMutations(TEST_SURVEY).test {
      assertThat(expectMostRecentItem()).isEqualTo(listOf(TEST_POLYGON_LOI_MUTATION))
    }
  }

  @Test
  fun testFindLocationsOfInterest() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION)

    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, FakeData.LOI_ID)

    localLoiStore.getValidLois(TEST_SURVEY).test {
      assertThat(expectMostRecentItem()).isEqualTo(setOf(loi))
    }
  }

  @Test
  fun testMergeLoi() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION)
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, FakeData.LOI_ID)!!
    val newLoi = loi.copy(geometry = TEST_POINT_2)
    localLoiStore.merge(newLoi)
    val newLoi2 = localLoiStore.getLocationOfInterest(TEST_SURVEY, FakeData.LOI_ID)
    assertThat(newLoi2?.geometry).isEqualTo(TEST_POINT_2)
  }

  @Test
  fun testMergePolygonLoi() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLoiStore.applyAndEnqueue(TEST_POLYGON_LOI_MUTATION)
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, FakeData.LOI_ID)!!
    val newLoi = loi.copy(geometry = Polygon(LinearRing(TEST_POLYGON_2)))
    localLoiStore.merge(newLoi)
    val newLoi2 = localLoiStore.getLocationOfInterest(TEST_SURVEY, FakeData.LOI_ID)
    assertThat(newLoi2?.geometry?.getShellCoordinates()).isEqualTo(TEST_POLYGON_2)
  }

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

  @Test
  fun testDeleteLoi() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER)
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY)
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION)
    localSubmissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION)

    // Assert that one LOI is streamed.
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, FakeData.LOI_ID)!!
    localLoiStore.getValidLois(TEST_SURVEY).test {
      assertThat(expectMostRecentItem()).isEqualTo(setOf(loi))
    }
    val mutation = TEST_LOI_MUTATION.copy(id = null, type = Mutation.Type.DELETE)

    // Calling applyAndEnqueue marks the local LOI as deleted.
    localLoiStore.applyAndEnqueue(mutation)

    // Verify that local entity exists but its state is updated to DELETED.
    assertThat(locationOfInterestDao.findById(FakeData.LOI_ID)?.deletionState)
      .isEqualTo(EntityDeletionState.DELETED)

    // Verify that the local LOI is now removed from the latest LOI stream.
    localLoiStore.getValidLois(TEST_SURVEY).test { assertThat(expectMostRecentItem()).isEmpty() }

    // After successful remote sync, delete LOI is called by LocalMutationSyncWorker.
    localLoiStore.deleteLocationOfInterest(FakeData.LOI_ID)

    // Verify that the LOI doesn't exist anymore
    assertThat(localLoiStore.getLocationOfInterest(TEST_SURVEY, FakeData.LOI_ID)).isNull()

    // Verify that the linked submission is also deleted.
    assertFailsWith<LocalDataStoreException> {
      localSubmissionStore.getSubmission(loi, "submission id")
    }
  }

  @Test
  fun testGetOfflineAreas() = runWithTestDispatcher {
    localOfflineAreaStore.insertOrUpdate(TEST_OFFLINE_AREA)
    localOfflineAreaStore.offlineAreas().test {
      assertThat(expectMostRecentItem()).isEqualTo(listOf(TEST_OFFLINE_AREA))
    }
  }

  @Test
  fun testParseVertices_emptyString() {
    assertThat(parseVertices("")).isEqualTo(listOf<Any>())
  }

  @Test
  fun testFormatVertices_emptyList() {
    assertThat(formatVertices(listOf())).isNull()
  }

  @Test
  fun testTermsOfServiceAccepted() {
    localValueStore.isTermsOfServiceAccepted = true
    assertThat(localValueStore.isTermsOfServiceAccepted).isTrue()
  }

  @Test
  fun testTermsOfServiceNotAccepted() {
    assertThat(localValueStore.isTermsOfServiceAccepted).isFalse()
  }

  companion object {
    private val TEST_USER = User(FakeData.USER_ID, "user@gmail.com", "user 1")
    private val TEST_TASK = Task("task id", 1, Task.Type.TEXT, "task label", false)
    private val TEST_STYLE = Style("#112233")
    private val TEST_JOB =
      Job(FakeData.JOB_ID, TEST_STYLE, "heading title", mapOf(Pair(TEST_TASK.id, TEST_TASK)))
    private val TEST_SURVEY =
      Survey(FakeData.SURVEY_ID, "survey 1", "foo description", mapOf(Pair(TEST_JOB.id, TEST_JOB)))
    private val TEST_POINT = Point(Coordinates(110.0, -23.1))
    private val TEST_POINT_2 = Point(Coordinates(51.0, 44.0))
    private val TEST_POLYGON_1 =
      listOf(
        Coordinates(49.874502, 8.655993),
        Coordinates(49.874099, 8.651173),
        Coordinates(49.872919, 8.651628),
        Coordinates(49.873164, 8.653515),
        Coordinates(49.874343, 8.653038),
        Coordinates(49.874502, 8.655993),
      )
    private val TEST_POLYGON_2 =
      listOf(
        Coordinates(49.865374, 8.646920),
        Coordinates(49.864241, 8.647286),
        Coordinates(49.864664, 8.650387),
        Coordinates(49.863102, 8.650445),
        Coordinates(49.863051, 8.647306),
        Coordinates(49.865374, 8.646920),
      )
    private val TEST_LOI_MUTATION = FakeData.newLoiMutation(TEST_POINT)
    private val TEST_POLYGON_LOI_MUTATION = FakeData.newAoiMutation(TEST_POLYGON_1)
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
    private val TEST_OFFLINE_AREA =
      OfflineArea("id_1", OfflineArea.State.PENDING, Bounds(0.0, 0.0, 0.0, 0.0), "Test Area", 0..14)

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
