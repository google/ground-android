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
package com.google.android.ground.persistence.local

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.Survey
import com.google.android.ground.model.User
import com.google.android.ground.model.basemap.OfflineArea
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.submission.TaskDataMap
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.room.converter.formatVertices
import com.google.android.ground.persistence.local.room.converter.parseVertices
import com.google.android.ground.persistence.local.room.dao.LocationOfInterestDao
import com.google.android.ground.persistence.local.room.dao.SubmissionDao
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionEntity
import com.google.android.ground.persistence.local.room.fields.EntityState
import com.google.android.ground.persistence.local.room.fields.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.stores.*
import com.google.android.ground.ui.map.Bounds
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocalDataStoreTests : BaseHiltTest() {
  // TODO(#1491): Split into multiple test suites, one for each SoT.
  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var localUserStore: LocalUserStore
  @Inject lateinit var localSubmissionStore: SubmissionStore
  @Inject lateinit var localLoiStore: LocalLocationOfInterestStore
  @Inject lateinit var localOfflineAreaStore: LocalOfflineAreaStore
  @Inject lateinit var localTileSetStore: LocalTileSetStore
  @Inject lateinit var localValueStore: LocalValueStore
  // TODO(#1470): Use public interface of data stores instead of inspecting state of impl (DAOs).
  @Inject lateinit var submissionDao: SubmissionDao
  @Inject lateinit var locationOfInterestDao: LocationOfInterestDao

  @Test
  fun testInsertAndGetSurveys() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).test().assertComplete()
    assertThat(localSurveyStore.surveys.first()).containsExactly(TEST_SURVEY)
  }

  @Test
  fun testGetSurveyById() {
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localSurveyStore.getSurveyById(TEST_SURVEY.id).test().assertValue(TEST_SURVEY)
  }

  @Test
  fun testDeleteSurvey() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localSurveyStore.deleteSurvey(TEST_SURVEY).test().assertComplete()
    assertThat(localSurveyStore.surveys.first()).isEmpty()
  }

  @Test
  fun testRemovedJobFromSurvey() {
    val job1 = Job("job 1", "job 1 name")
    val job2 = Job("job 2", "job 2 name")
    var survey =
      Survey("foo id", "foo survey", "foo survey description", mapOf(Pair(job1.id, job1)))
    localSurveyStore.insertOrUpdateSurvey(survey).blockingAwait()
    survey = Survey("foo id", "foo survey", "foo survey description", mapOf(Pair(job2.id, job2)))
    localSurveyStore.insertOrUpdateSurvey(survey).blockingAwait()
    localSurveyStore.getSurveyById("foo id").test().assertValue { result: Survey ->
      result.jobs.size == 1 && result.jobs.first() == job2
    }
  }

  @Test
  fun testInsertAndGetUser() {
    localUserStore.insertOrUpdateUser(TEST_USER).test().assertComplete()
    localUserStore.getUser("user id").test().assertValue(TEST_USER)
  }

  @Test
  fun testApplyAndEnqueue_insertsLoi() {
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()

    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION).test().assertComplete()

    localLoiStore
      .getLocationOfInterest(TEST_SURVEY, TEST_LOI_MUTATION.locationOfInterestId)
      .test()
      .assertValue { it.geometry == TEST_POINT }
  }
  @Test
  fun testApplyAndEnqueue_insertsMutation() = runWithTestDispatcher {
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()

    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION).test().assertComplete()
    advanceUntilIdle()

    localLoiStore
      .getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
        TEST_LOI_MUTATION.locationOfInterestId,
        MutationEntitySyncStatus.PENDING
      )
      .test()
      .assertValue(listOf(TEST_LOI_MUTATION))
  }

  @Test
  fun testApplyAndEnqueue_insertPolygonLoi() {
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()

    localLoiStore.applyAndEnqueue(TEST_POLYGON_LOI_MUTATION).test().assertComplete()

    localLoiStore
      .getLocationOfInterest(TEST_SURVEY, TEST_POLYGON_LOI_MUTATION.locationOfInterestId)
      .test()
      .assertValue { it.geometry == TEST_POLYGON_LOI_MUTATION.geometry }
  }

  @Test
  fun testApplyAndEnqueue_enqueuesPolygonLoiMutation() {
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localLoiStore.applyAndEnqueue(TEST_POLYGON_LOI_MUTATION).test().assertComplete()

    localLoiStore
      .getLocationOfInterestMutationsByLocationOfInterestIdOnceAndStream(
        TEST_POLYGON_LOI_MUTATION.locationOfInterestId,
        MutationEntitySyncStatus.PENDING
      )
      .test()
      .assertValue(listOf(TEST_POLYGON_LOI_MUTATION))
  }

  @Test
  fun testGetLoisOnceAndStream() {
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    val subscriber = localLoiStore.getLocationsOfInterestOnceAndStream(TEST_SURVEY).test()
    subscriber.assertValue(setOf())
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
    subscriber.assertValueSet(setOf(setOf(), setOf(loi)))
  }

  @Test
  fun testMergeLoi() {
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
    val newLoi = loi.copy(geometry = TEST_POINT_2)
    localLoiStore.merge(newLoi).test().assertComplete()
    localLoiStore.getLocationOfInterest(TEST_SURVEY, "loi id").test().assertValue {
      it.geometry == TEST_POINT_2
    }
  }

  @Test
  fun testMergePolygonLoi() {
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localLoiStore.applyAndEnqueue(TEST_POLYGON_LOI_MUTATION).blockingAwait()
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
    val newLoi = loi.copy(geometry = Polygon(LinearRing(TEST_POLYGON_2.map { it.coordinate })))
    localLoiStore.merge(newLoi).test().assertComplete()
    localLoiStore.getLocationOfInterest(TEST_SURVEY, "loi id").test().assertValue {
      it.geometry.vertices == TEST_POLYGON_2
    }
  }

  @Test
  fun testApplyAndEnqueue_insertAndUpdateSubmission() {
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()

    localSubmissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).test().assertComplete()

    localSubmissionStore
      .getSubmissionMutationsByLocationOfInterestIdOnceAndStream(
        TEST_SURVEY,
        TEST_LOI_MUTATION.locationOfInterestId,
        MutationEntitySyncStatus.PENDING
      )
      .test()
      .assertValue(listOf(TEST_SUBMISSION_MUTATION))
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
    var submission = localSubmissionStore.getSubmission(loi, "submission id").blockingGet()
    assertEquivalent(TEST_SUBMISSION_MUTATION, submission)

    // now update the inserted submission with new responses
    val deltas =
      listOf(
        TaskDataDelta(
          "task id",
          Task.Type.TEXT,
          TextTaskData.fromString("value for the really new task")
        )
      )
    val mutation =
      TEST_SUBMISSION_MUTATION.copy(taskDataDeltas = deltas, id = 2L, type = Mutation.Type.UPDATE)

    localSubmissionStore.applyAndEnqueue(mutation).test().assertComplete()

    localSubmissionStore
      .getSubmissionMutationsByLocationOfInterestIdOnceAndStream(
        TEST_SURVEY,
        TEST_LOI_MUTATION.locationOfInterestId,
        MutationEntitySyncStatus.PENDING
      )
      .test()
      .assertValue(listOf(TEST_SUBMISSION_MUTATION, mutation))

    // check if the submission was updated in the local database
    submission = localSubmissionStore.getSubmission(loi, "submission id").blockingGet()
    assertEquivalent(mutation, submission)

    // also test that getSubmissions returns the same submission as well
    val submissions = localSubmissionStore.getSubmissions(loi, "job id").blockingGet()
    assertThat(submissions).hasSize(1)
    assertEquivalent(mutation, submissions[0])
  }

  @Test
  fun testMergeSubmission() {
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    localSubmissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait()
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
    val taskDataMap =
      TaskDataMap(mapOf(Pair("task id", TextTaskData.fromString("foo value").get())))
    val submission =
      localSubmissionStore
        .getSubmission(loi, "submission id")
        .blockingGet()
        .copy(responses = taskDataMap)
    localSubmissionStore.merge(submission).test().assertComplete()
    val responses =
      localSubmissionStore.getSubmission(loi, submission.id).test().values()[0].responses
    assertThat(responses.getResponse("task id"))
      .isEqualTo(TextTaskData.fromString("updated taskData"))
  }

  @Test
  fun testDeleteSubmission() {
    // Add test submission
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    localSubmissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait()
    val mutation = TEST_SUBMISSION_MUTATION.copy(id = null, type = Mutation.Type.DELETE)

    // Calling applyAndEnqueue marks the local submission as deleted.
    localSubmissionStore.applyAndEnqueue(mutation).blockingAwait()

    // Verify that local entity exists and its state is updated.
    submissionDao.findById("submission id").test().assertValue { submissionEntity: SubmissionEntity
      ->
      submissionEntity.state == EntityState.DELETED
    }

    // Verify that the local submission doesn't end up in getSubmissions().
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
    localSubmissionStore.getSubmissions(loi, "task id").test().assertValue(listOf())

    // After successful remote sync, delete submission is called by LocalMutationSyncWorker.
    localSubmissionStore.deleteSubmission("submission id").blockingAwait()

    // Verify that the submission doesn't exist anymore
    localSubmissionStore.getSubmission(loi, "submission id").test().assertNoValues()
  }

  @Test
  fun testDeleteLoi() {
    localUserStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localSurveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localLoiStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    localSubmissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait()
    val subscriber = localLoiStore.getLocationsOfInterestOnceAndStream(TEST_SURVEY).test()

    // Assert that one LOI is streamed.
    val loi = localLoiStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
    subscriber.assertValueAt(0, setOf(loi))
    val mutation = TEST_LOI_MUTATION.copy(id = null, type = Mutation.Type.DELETE)

    // Calling applyAndEnqueue marks the local LOI as deleted.
    localLoiStore.applyAndEnqueue(mutation).blockingAwait()

    // Verify that local entity exists but its state is updated to DELETED.
    locationOfInterestDao.findById("loi id").test().assertValue { entity: LocationOfInterestEntity
      ->
      entity.state == EntityState.DELETED
    }

    // Verify that the local LOI is now removed from the latest LOI stream.
    subscriber.assertValueAt(1, setOf())

    // After successful remote sync, delete LOI is called by LocalMutationSyncWorker.
    localLoiStore.deleteLocationOfInterest("loi id").blockingAwait()

    // Verify that the LOI doesn't exist anymore
    localLoiStore.getLocationOfInterest(TEST_SURVEY, "loi id").test().assertNoValues()

    // Verify that the linked submission is also deleted.
    localSubmissionStore.getSubmission(loi, "submission id").test().assertNoValues()
  }

  @Test
  fun testInsertTile() {
    localTileSetStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).test().assertComplete()
  }

  @Test
  fun testGetTile() {
    localTileSetStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait()
    localTileSetStore
      .getTileSet("some_url 1")
      .test()
      .assertValueCount(1)
      .assertValue(TEST_PENDING_TILE_SOURCE)
  }

  @Test
  fun testGetTilesOnceAndStream() {
    val subscriber = localTileSetStore.tileSetsOnceAndStream().test()
    subscriber.assertValue(setOf())
    localTileSetStore.insertOrUpdateTileSet(TEST_DOWNLOADED_TILE_SOURCE).blockingAwait()
    localTileSetStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait()
    subscriber.assertValueSet(
      setOf(
        setOf(),
        setOf(TEST_DOWNLOADED_TILE_SOURCE),
        setOf(TEST_DOWNLOADED_TILE_SOURCE, TEST_PENDING_TILE_SOURCE)
      )
    )
  }

  @Test
  fun testGetPendingTile() {
    localTileSetStore.insertOrUpdateTileSet(TEST_DOWNLOADED_TILE_SOURCE).blockingAwait()
    localTileSetStore.insertOrUpdateTileSet(TEST_FAILED_TILE_SOURCE).blockingAwait()
    localTileSetStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait()
    localTileSetStore.pendingTileSets().test().assertValue(listOf(TEST_PENDING_TILE_SOURCE))
  }

  @Test
  fun testInsertOfflineArea() {
    localOfflineAreaStore.insertOrUpdateOfflineArea(TEST_OFFLINE_AREA).test().assertComplete()
  }

  @Test
  fun testGetOfflineAreas() {
    localOfflineAreaStore.insertOrUpdateOfflineArea(TEST_OFFLINE_AREA).blockingAwait()

    localOfflineAreaStore.offlineAreasOnceAndStream().test().assertValue(listOf(TEST_OFFLINE_AREA))
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
    private val TEST_USER = User("user id", "user@gmail.com", "user 1")
    private val TEST_TASK = Task("task id", 1, Task.Type.TEXT, "task label", false)
    private val TEST_JOB = Job("job id", "heading title", mapOf(Pair(TEST_TASK.id, TEST_TASK)))
    private val TEST_SURVEY =
      Survey("survey id", "survey 1", "foo description", mapOf(Pair(TEST_JOB.id, TEST_JOB)))
    private val TEST_POINT = Point(Coordinate(110.0, -23.1))
    private val TEST_POINT_2 = Point(Coordinate(51.0, 44.0))
    private val TEST_POLYGON_1 =
      listOf(
        Point(Coordinate(49.874502, 8.655993)),
        Point(Coordinate(49.874099, 8.651173)),
        Point(Coordinate(49.872919, 8.651628)),
        Point(Coordinate(49.873164, 8.653515)),
        Point(Coordinate(49.874343, 8.653038)),
        Point(Coordinate(49.874502, 8.655993))
      )
    private val TEST_POLYGON_2 =
      listOf(
        Point(Coordinate(49.865374, 8.646920)),
        Point(Coordinate(49.864241, 8.647286)),
        Point(Coordinate(49.864664, 8.650387)),
        Point(Coordinate(49.863102, 8.650445)),
        Point(Coordinate(49.863051, 8.647306)),
        Point(Coordinate(49.865374, 8.646920))
      )
    private val TEST_LOI_MUTATION = createTestLocationOfInterestMutation(TEST_POINT)
    private val TEST_POLYGON_LOI_MUTATION = createTestAreaOfInterestMutation(TEST_POLYGON_1)
    private val TEST_SUBMISSION_MUTATION =
      SubmissionMutation(
        job = TEST_JOB,
        submissionId = "submission id",
        taskDataDeltas =
          listOf(
            TaskDataDelta("task id", Task.Type.TEXT, TextTaskData.fromString("updated taskData"))
          ),
        id = 1L,
        type = Mutation.Type.CREATE,
        syncStatus = SyncStatus.PENDING,
        surveyId = "survey id",
        locationOfInterestId = "loi id",
        userId = "user id"
      )
    private val TEST_PENDING_TILE_SOURCE =
      TileSet("some_url 1", "id_1", "some_path 1", TileSet.State.PENDING, 1)
    private val TEST_DOWNLOADED_TILE_SOURCE =
      TileSet("some_url 2", "id_2", "some_path 2", TileSet.State.DOWNLOADED, 1)
    private val TEST_FAILED_TILE_SOURCE =
      TileSet("some_url 3", "id_3", "some_path 3", TileSet.State.FAILED, 1)
    private val TEST_OFFLINE_AREA =
      OfflineArea(
        "id_1",
        OfflineArea.State.PENDING,
        Bounds(Coordinate(0.0, 0.0), Coordinate(0.0, 0.0)),
        "Test Area"
      )

    private fun createTestLocationOfInterestMutation(point: Point): LocationOfInterestMutation =
      LocationOfInterestMutation(
        jobId = "job id",
        geometry = point,
        id = 1L,
        locationOfInterestId = "loi id",
        type = Mutation.Type.CREATE,
        syncStatus = SyncStatus.PENDING,
        userId = "user id",
        surveyId = "survey id",
        clientTimestamp = Date()
      )

    private fun createTestAreaOfInterestMutation(
      polygonVertices: List<Point>
    ): LocationOfInterestMutation =
      LocationOfInterestMutation(
        jobId = "job id",
        geometry = Polygon(LinearRing(polygonVertices.map { it.coordinate })),
        id = 1L,
        locationOfInterestId = "loi id",
        type = Mutation.Type.CREATE,
        syncStatus = SyncStatus.PENDING,
        userId = "user id",
        surveyId = "survey id",
        clientTimestamp = Date()
      )

    private fun assertEquivalent(mutation: SubmissionMutation, submission: Submission) {
      assertThat(mutation.submissionId).isEqualTo(submission.id)
      assertThat(mutation.locationOfInterestId).isEqualTo(submission.locationOfInterest.id)
      assertThat(mutation.job).isEqualTo(submission.job)
      assertThat(mutation.surveyId).isEqualTo(submission.surveyId)
      assertThat(mutation.userId).isEqualTo(submission.lastModified.user.id)
      assertThat(mutation.userId).isEqualTo(submission.created.user.id)
      MatcherAssert.assertThat(
        TaskDataMap().copyWithDeltas(mutation.taskDataDeltas),
        Matchers.samePropertyValuesAs(submission.responses)
      )
    }
  }
}
