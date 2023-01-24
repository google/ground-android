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

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
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
import com.google.android.ground.model.locationofinterest.LocationOfInterest
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
import com.google.android.ground.persistence.local.room.converter.toLocalDataStoreObject
import com.google.android.ground.persistence.local.room.dao.LocationOfInterestDao
import com.google.android.ground.persistence.local.room.dao.SubmissionDao
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestEntity
import com.google.android.ground.persistence.local.room.entity.SubmissionEntity
import com.google.android.ground.persistence.local.room.models.EntityState
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.*
import javax.inject.Inject
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocalDataLocalStoreTest : BaseHiltTest() {
  @Inject lateinit var localDataStore: LocalDataStore
  @Inject lateinit var localValueStore: LocalValueStore
  @Inject lateinit var submissionDao: SubmissionDao
  @Inject lateinit var locationOfInterestDao: LocationOfInterestDao

  @Test
  fun testInsertAndGetSurveys() {
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).test().assertComplete()
    localDataStore.surveyStore.surveys.test().assertValue(ImmutableList.of(TEST_SURVEY))
  }

  @Test
  fun testGetSurveyById() {
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.surveyStore.getSurveyById(TEST_SURVEY.id).test().assertValue(TEST_SURVEY)
  }

  @Test
  fun testDeleteSurvey() {
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.surveyStore.deleteSurvey(TEST_SURVEY).test().assertComplete()
    localDataStore.surveyStore.surveys.test().assertValue { obj: ImmutableList<Survey> ->
      obj.isEmpty()
    }
  }

  @Test
  fun testRemovedJobFromSurvey() {
    val job1 = Job("job 1", "job 1 name")
    val job2 = Job("job 2", "job 2 name")
    var survey =
      Survey(
        "foo id",
        "foo survey",
        "foo survey description",
        ImmutableMap.builder<String, Job>().put(job1.id, job1).build()
      )
    localDataStore.surveyStore.insertOrUpdateSurvey(survey).blockingAwait()
    survey =
      Survey(
        "foo id",
        "foo survey",
        "foo survey description",
        ImmutableMap.builder<String, Job>().put(job2.id, job2).build()
      )
    localDataStore.surveyStore.insertOrUpdateSurvey(survey).blockingAwait()
    localDataStore.surveyStore.getSurveyById("foo id").test().assertValue { result: Survey ->
      result.jobs == ImmutableList.of(job2)
    }
  }

  @Test
  fun testInsertAndGetUser() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).test().assertComplete()
    localDataStore.userStore.getUser("user id").test().assertValue(TEST_USER)
  }

  @Test
  fun testApplyAndEnqueue_loiMutation() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore
      .applyAndEnqueue(TEST_LOI_MUTATION)
      .test()
      .assertComplete()

    // assert that mutation is saved to local database
    localDataStore
      .getPendingMutations("loi id")
      .test()
      .assertValue(ImmutableList.of(TEST_LOI_MUTATION))
    localDataStore.localLocationOfInterestStore
      .getLocationOfInterest(TEST_SURVEY, "loi id")
      .test()
      .assertValue { loi: LocationOfInterest -> loi.geometry == TEST_POINT }
  }

  @Test
  fun testApplyAndEnqueue_polygonLoiMutation() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore
      .applyAndEnqueue(TEST_POLYGON_LOI_MUTATION)
      .test()
      .assertComplete()

    // assert that mutation is saved to local database
    localDataStore
      .getPendingMutations("loi id")
      .test()
      .assertValue(ImmutableList.of(TEST_POLYGON_LOI_MUTATION))
    localDataStore.localLocationOfInterestStore
      .getLocationOfInterest(TEST_SURVEY, "loi id")
      .test()
      .assertValue { loi: LocationOfInterest -> loi.geometry.vertices == TEST_POLYGON_1 }
  }

  @Test
  fun testGetLoisOnceAndStream() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    val subscriber =
      localDataStore.localLocationOfInterestStore
        .getLocationsOfInterestOnceAndStream(TEST_SURVEY)
        .test()
    subscriber.assertValue(ImmutableSet.of())
    localDataStore.localLocationOfInterestStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    val loi =
      localDataStore.localLocationOfInterestStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .blockingGet()
    subscriber.assertValueSet(ImmutableSet.of(ImmutableSet.of(), ImmutableSet.of(loi)))
  }

  @Test
  fun testUpdateMutations() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    val mutation = createTestLocationOfInterestMutation(TEST_POINT_2)
    localDataStore.updateMutations(ImmutableList.of(mutation)).test().assertComplete()
    localDataStore
      .getPendingMutations(TEST_LOI_MUTATION.locationOfInterestId)
      .test()
      .assertValue(ImmutableList.of(mutation))
  }

  @Test
  fun testPolygonUpdateMutations() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore
      .applyAndEnqueue(TEST_POLYGON_LOI_MUTATION)
      .blockingAwait()
    val mutation = createTestAreaOfInterestMutation(TEST_POLYGON_2)
    localDataStore.updateMutations(ImmutableList.of(mutation)).test().assertComplete()
    localDataStore
      .getPendingMutations(TEST_POLYGON_LOI_MUTATION.locationOfInterestId)
      .test()
      .assertValue(ImmutableList.of(mutation))
  }

  @Test
  fun testFinalizePendingMutation() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    localDataStore
      .finalizePendingMutations(ImmutableList.of(TEST_LOI_MUTATION))
      .test()
      .assertComplete()
    localDataStore.getPendingMutations("loi id").test().assertValue { it.isEmpty() }
  }

  @Test
  fun testMergeLoi() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    val loi =
      localDataStore.localLocationOfInterestStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .blockingGet()
    val newLoi = loi.copy(geometry = TEST_POINT_2)
    localDataStore.localLocationOfInterestStore.merge(newLoi).test().assertComplete()
    localDataStore.localLocationOfInterestStore
      .getLocationOfInterest(TEST_SURVEY, "loi id")
      .test()
      .assertValue { it.geometry == TEST_POINT_2 }
  }

  @Test
  fun testMergePolygonLoi() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore
      .applyAndEnqueue(TEST_POLYGON_LOI_MUTATION)
      .blockingAwait()
    val loi =
      localDataStore.localLocationOfInterestStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .blockingGet()
    val newLoi = loi.copy(geometry = Polygon(LinearRing(TEST_POLYGON_2.map { it.coordinate })))
    localDataStore.localLocationOfInterestStore.merge(newLoi).test().assertComplete()
    localDataStore.localLocationOfInterestStore
      .getLocationOfInterest(TEST_SURVEY, "loi id")
      .test()
      .assertValue { it.geometry.vertices == TEST_POLYGON_2 }
  }

  @Test
  fun testApplyAndEnqueue_submissionMutation() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    localDataStore.submissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).test().assertComplete()
    localDataStore
      .getPendingMutations("loi id")
      .test()
      .assertValue(ImmutableList.of(TEST_LOI_MUTATION, TEST_SUBMISSION_MUTATION))
    val loi =
      localDataStore.localLocationOfInterestStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .blockingGet()
    var submission =
      localDataStore.submissionStore.getSubmission(loi, "submission id").blockingGet()
    assertEquivalent(TEST_SUBMISSION_MUTATION, submission)

    // now update the inserted submission with new responses
    val deltas =
      ImmutableList.of(
        TaskDataDelta(
          "task id",
          Task.Type.TEXT,
          TextTaskData.fromString("value for the really new task")
        )
      )
    val mutation =
      TEST_SUBMISSION_MUTATION.copy(taskDataDeltas = deltas, id = 2L, type = Mutation.Type.UPDATE)
    localDataStore.submissionStore.applyAndEnqueue(mutation).test().assertComplete()
    localDataStore
      .getPendingMutations("loi id")
      .test()
      .assertValue(ImmutableList.of(TEST_LOI_MUTATION, TEST_SUBMISSION_MUTATION, mutation))

    // check if the submission was updated in the local database
    submission = localDataStore.submissionStore.getSubmission(loi, "submission id").blockingGet()
    assertEquivalent(mutation, submission)

    // also test that getSubmissions returns the same submission as well
    val submissions = localDataStore.submissionStore.getSubmissions(loi, "job id").blockingGet()
    assertThat(submissions).hasSize(1)
    assertEquivalent(mutation, submissions[0])
  }

  @Test
  fun testMergeSubmission() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    localDataStore.submissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait()
    val loi =
      localDataStore.localLocationOfInterestStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .blockingGet()
    val taskDataMap =
      TaskDataMap(ImmutableMap.of("task id", TextTaskData.fromString("foo value").get()))
    val submission =
      localDataStore.submissionStore
        .getSubmission(loi, "submission id")
        .blockingGet()
        .copy(responses = taskDataMap)
    localDataStore.submissionStore.merge(submission).test().assertComplete()
    val responses =
      localDataStore.submissionStore.getSubmission(loi, submission.id).test().values()[0].responses
    assertThat(responses.getResponse("task id"))
      .isEqualTo(TextTaskData.fromString("updated taskData"))
  }

  @Test
  fun testDeleteSubmission() {
    // Add test submission
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    localDataStore.submissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait()
    val mutation = TEST_SUBMISSION_MUTATION.copy(id = null, type = Mutation.Type.DELETE)

    // Calling applyAndEnqueue marks the local submission as deleted.
    localDataStore.submissionStore.applyAndEnqueue(mutation).blockingAwait()

    // Verify that local entity exists and its state is updated.
    submissionDao.findById("submission id").test().assertValue { submissionEntity: SubmissionEntity
      ->
      submissionEntity.state == EntityState.DELETED
    }

    // Verify that the local submission doesn't end up in getSubmissions().
    val loi =
      localDataStore.localLocationOfInterestStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .blockingGet()
    localDataStore.submissionStore
      .getSubmissions(loi, "task id")
      .test()
      .assertValue(ImmutableList.of())

    // After successful remote sync, delete submission is called by LocalMutationSyncWorker.
    localDataStore.submissionStore.deleteSubmission("submission id").blockingAwait()

    // Verify that the submission doesn't exist anymore
    localDataStore.submissionStore.getSubmission(loi, "submission id").test().assertNoValues()
  }

  @Test
  fun testDeleteLoi() {
    localDataStore.userStore.insertOrUpdateUser(TEST_USER).blockingAwait()
    localDataStore.surveyStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
    localDataStore.localLocationOfInterestStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
    localDataStore.submissionStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait()
    val subscriber =
      localDataStore.localLocationOfInterestStore
        .getLocationsOfInterestOnceAndStream(TEST_SURVEY)
        .test()

    // Assert that one LOI is streamed.
    val loi =
      localDataStore.localLocationOfInterestStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .blockingGet()
    subscriber.assertValueAt(0, ImmutableSet.of(loi))
    val mutation = TEST_LOI_MUTATION.copy(id = null, type = Mutation.Type.DELETE)

    // Calling applyAndEnqueue marks the local LOI as deleted.
    localDataStore.localLocationOfInterestStore.applyAndEnqueue(mutation).blockingAwait()

    // Verify that local entity exists but its state is updated to DELETED.
    locationOfInterestDao.findById("loi id").test().assertValue { entity: LocationOfInterestEntity
      ->
      entity.state == EntityState.DELETED
    }

    // Verify that the local LOI is now removed from the latest LOI stream.
    subscriber.assertValueAt(1, ImmutableSet.of())

    // After successful remote sync, delete LOI is called by LocalMutationSyncWorker.
    localDataStore.localLocationOfInterestStore.deleteLocationOfInterest("loi id").blockingAwait()

    // Verify that the LOI doesn't exist anymore
    localDataStore.localLocationOfInterestStore
      .getLocationOfInterest(TEST_SURVEY, "loi id")
      .test()
      .assertNoValues()

    // Verify that the linked submission is also deleted.
    localDataStore.submissionStore.getSubmission(loi, "submission id").test().assertNoValues()
  }

  @Test
  fun testInsertTile() {
    localDataStore.tileSetStore
      .insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE)
      .test()
      .assertComplete()
  }

  @Test
  fun testGetTile() {
    localDataStore.tileSetStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait()
    localDataStore.tileSetStore
      .getTileSet("some_url 1")
      .test()
      .assertValueCount(1)
      .assertValue(TEST_PENDING_TILE_SOURCE)
  }

  @Test
  fun testGetTilesOnceAndStream() {
    val subscriber = localDataStore.tileSetStore.tileSetsOnceAndStream.test()
    subscriber.assertValue(ImmutableSet.of())
    localDataStore.tileSetStore.insertOrUpdateTileSet(TEST_DOWNLOADED_TILE_SOURCE).blockingAwait()
    localDataStore.tileSetStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait()
    subscriber.assertValueSet(
      ImmutableSet.of(
        ImmutableSet.of(),
        ImmutableSet.of(TEST_DOWNLOADED_TILE_SOURCE),
        ImmutableSet.of(TEST_DOWNLOADED_TILE_SOURCE, TEST_PENDING_TILE_SOURCE)
      )
    )
  }

  @Test
  fun testGetPendingTile() {
    localDataStore.tileSetStore.insertOrUpdateTileSet(TEST_DOWNLOADED_TILE_SOURCE).blockingAwait()
    localDataStore.tileSetStore.insertOrUpdateTileSet(TEST_FAILED_TILE_SOURCE).blockingAwait()
    localDataStore.tileSetStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait()
    localDataStore.tileSetStore.pendingTileSets
      .test()
      .assertValue(ImmutableList.of(TEST_PENDING_TILE_SOURCE))
  }

  @Test
  fun testInsertOfflineArea() {
    localDataStore.localOfflineAreaStore
      .insertOrUpdateOfflineArea(TEST_OFFLINE_AREA)
      .test()
      .assertComplete()
  }

  @Test
  fun testGetOfflineAreas() {
    localDataStore.localOfflineAreaStore
      .insertOrUpdateOfflineArea(TEST_OFFLINE_AREA)
      .blockingAwait()
    localDataStore.localOfflineAreaStore.offlineAreasOnceAndStream
      .test()
      .assertValue(ImmutableList.of(TEST_OFFLINE_AREA))
  }

  @Test
  fun testParseVertices_emptyString() {
    assertThat(parseVertices("")).isEqualTo(ImmutableList.of<Any>())
  }

  @Test
  fun testFormatVertices_emptyList() {
    assertThat(formatVertices(ImmutableList.of())).isNull()
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
    private val TEST_JOB =
      Job(
        "job id",
        "heading title",
        ImmutableMap.builder<String, Task>().put(TEST_TASK.id, TEST_TASK).build()
      )
    private val TEST_SURVEY =
      Survey(
        "survey id",
        "survey 1",
        "foo description",
        ImmutableMap.builder<String, Job>().put(TEST_JOB.id, TEST_JOB).build()
      )
    private val TEST_POINT = Point(Coordinate(110.0, -23.1))
    private val TEST_POINT_2 = Point(Coordinate(51.0, 44.0))
    private val TEST_POLYGON_1 =
      ImmutableList.builder<Point>()
        .add(Point(Coordinate(49.874502, 8.655993)))
        .add(Point(Coordinate(49.874099, 8.651173)))
        .add(Point(Coordinate(49.872919, 8.651628)))
        .add(Point(Coordinate(49.873164, 8.653515)))
        .add(Point(Coordinate(49.874343, 8.653038)))
        .add(Point(Coordinate(49.874502, 8.655993)))
        .build()
    private val TEST_POLYGON_2 =
      ImmutableList.builder<Point>()
        .add(Point(Coordinate(49.865374, 8.646920)))
        .add(Point(Coordinate(49.864241, 8.647286)))
        .add(Point(Coordinate(49.864664, 8.650387)))
        .add(Point(Coordinate(49.863102, 8.650445)))
        .add(Point(Coordinate(49.863051, 8.647306)))
        .add(Point(Coordinate(49.865374, 8.646920)))
        .build()
    private val TEST_LOI_MUTATION = createTestLocationOfInterestMutation(TEST_POINT)
    private val TEST_POLYGON_LOI_MUTATION = createTestAreaOfInterestMutation(TEST_POLYGON_1)
    private val TEST_SUBMISSION_MUTATION =
      SubmissionMutation(
        job = TEST_JOB,
        submissionId = "submission id",
        taskDataDeltas =
          ImmutableList.of(
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
        LatLngBounds.builder().include(LatLng(0.0, 0.0)).build(),
        "Test Area"
      )

    private fun createTestLocationOfInterestMutation(point: Point): LocationOfInterestMutation =
      LocationOfInterestMutation(
        jobId = "job id",
        geometry = point.toLocalDataStoreObject(),
        id = 1L,
        locationOfInterestId = "loi id",
        type = Mutation.Type.CREATE,
        syncStatus = SyncStatus.PENDING,
        userId = "user id",
        surveyId = "survey id",
        clientTimestamp = Date()
      )

    private fun createTestAreaOfInterestMutation(
      polygonVertices: ImmutableList<Point>
    ): LocationOfInterestMutation =
      LocationOfInterestMutation(
        jobId = "job id",
        geometry =
          Polygon(LinearRing(polygonVertices.map { it.coordinate })).toLocalDataStoreObject(),
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
