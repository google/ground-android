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
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.locationofinterest.Point
import com.google.android.ground.model.locationofinterest.toPolygon
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.Mutation.SyncStatus
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.ResponseDelta
import com.google.android.ground.model.submission.ResponseMap
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TextResponse
import com.google.android.ground.model.task.Task
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
import java8.util.Optional
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocalDataStoreTest : BaseHiltTest() {
    @Inject
    lateinit var localDataStore: LocalDataStore

    @Inject
    lateinit var localValueStore: LocalValueStore

    @Inject
    lateinit var submissionDao: SubmissionDao

    @Inject
    lateinit var locationOfInterestDao: LocationOfInterestDao

    @Test
    fun testInsertAndGetSurveys() {
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).test().assertComplete()
        localDataStore.surveys.test().assertValue(ImmutableList.of(TEST_SURVEY))
    }

    @Test
    fun testGetSurveyById() {
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.getSurveyById(TEST_SURVEY.id).test().assertValue(TEST_SURVEY)
    }

    @Test
    fun testDeleteSurvey() {
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.deleteSurvey(TEST_SURVEY).test().assertComplete()
        localDataStore.surveys.test().assertValue { obj: ImmutableList<Survey?> -> obj.isEmpty() }
    }

    @Test
    fun testRemovedJobFromSurvey() {
        val job1 = Job("job 1", "job 1 name")
        val job2 = Job("job 2", "job 2 name")
        var survey = Survey(
            "foo id",
            "foo survey",
            "foo survey description",
            ImmutableMap.builder<String, Job>().put(job1.id, job1).build()
        )
        localDataStore.insertOrUpdateSurvey(survey).blockingAwait()
        survey = Survey(
            "foo id",
            "foo survey",
            "foo survey description",
            ImmutableMap.builder<String, Job>().put(job2.id, job2).build()
        )
        localDataStore.insertOrUpdateSurvey(survey).blockingAwait()
        localDataStore
            .getSurveyById("foo id")
            .test()
            .assertValue { result: Survey -> result.jobs == ImmutableList.of(job2) }
    }

    @Test
    fun testInsertAndGetUser() {
        localDataStore.insertOrUpdateUser(TEST_USER).test().assertComplete()
        localDataStore.getUser("user id").test().assertValue(TEST_USER)
    }

    @Test
    fun testApplyAndEnqueue_loiMutation() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_LOI_MUTATION).test().assertComplete()

        // assert that mutation is saved to local database
        localDataStore
            .getPendingMutations("loi id")
            .test()
            .assertValue(ImmutableList.of(TEST_LOI_MUTATION))
        localDataStore
            .getLocationOfInterest(TEST_SURVEY, "loi id")
            .test()
            .assertValue { loi: LocationOfInterest -> loi.coordinatesAsPoint == TEST_POINT }
    }

    @Test
    fun testApplyAndEnqueue_polygonLoiMutation() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_POLYGON_LOI_MUTATION).test().assertComplete()

        // assert that mutation is saved to local database
        localDataStore
            .getPendingMutations("loi id")
            .test()
            .assertValue(ImmutableList.of(TEST_POLYGON_LOI_MUTATION))
        localDataStore
            .getLocationOfInterest(TEST_SURVEY, "loi id")
            .test()
            .assertValue { loi: LocationOfInterest -> loi.coordinatesAsPoints == TEST_POLYGON_1 }
    }

    @Test
    fun testGetLoisOnceAndStream() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        val subscriber = localDataStore.getLocationsOfInterestOnceAndStream(TEST_SURVEY).test()
        subscriber.assertValue(ImmutableSet.of())
        localDataStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
        val loi = localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
        subscriber.assertValueSet(ImmutableSet.of(ImmutableSet.of(), ImmutableSet.of(loi)))
    }

    @Test
    fun testUpdateMutations() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
        val mutation = createTestLocationOfInterestMutation(TEST_POINT_2)
        localDataStore.updateMutations(ImmutableList.of(mutation)).test().assertComplete()
        localDataStore
            .getPendingMutations(TEST_LOI_MUTATION.locationOfInterestId)
            .test()
            .assertValue(ImmutableList.of(mutation))
    }

    @Test
    fun testPolygonUpdateMutations() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_POLYGON_LOI_MUTATION).blockingAwait()
        val mutation = createTestAreaOfInterestMutation(TEST_POLYGON_2)
        localDataStore.updateMutations(ImmutableList.of(mutation)).test().assertComplete()
        localDataStore
            .getPendingMutations(TEST_POLYGON_LOI_MUTATION.locationOfInterestId)
            .test()
            .assertValue(ImmutableList.of(mutation))
    }

    @Test
    fun testFinalizePendingMutation() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
        localDataStore
            .finalizePendingMutations(ImmutableList.of(TEST_LOI_MUTATION))
            .test()
            .assertComplete()
        localDataStore.getPendingMutations("loi id").test()
            .assertValue { obj: ImmutableList<Mutation?> -> obj.isEmpty() }
    }

    @Test
    fun testMergeLoi() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
        val loi = localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
        val newLoi = loi.copy(
            loi.id,
            loi.survey,
            loi.job,
            loi.customId,
            loi.caption,
            loi.created,
            loi.lastModified,
            TEST_POINT_2.toGeometry()
        )
        localDataStore.mergeLocationOfInterest(newLoi).test().assertComplete()
        localDataStore
            .getLocationOfInterest(TEST_SURVEY, "loi id")
            .test()
            .assertValue { it.coordinatesAsPoint == TEST_POINT_2 }
    }

    @Test
    fun testMergePolygonLoi() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_POLYGON_LOI_MUTATION).blockingAwait()
        val loi = localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
        val newLoi = loi.copy(
            loi.id,
            loi.survey,
            loi.job,
            loi.customId,
            loi.caption,
            loi.created,
            loi.lastModified,
            TEST_POLYGON_2.toPolygon()
        )
        localDataStore.mergeLocationOfInterest(newLoi).test().assertComplete()
        localDataStore
            .getLocationOfInterest(TEST_SURVEY, "loi id")
            .test()
            .assertValue { it.coordinatesAsPoints == TEST_POLYGON_2 }
    }

    @Test
    fun testApplyAndEnqueue_submissionMutation() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).test().assertComplete()
        localDataStore
            .getPendingMutations("loi id")
            .test()
            .assertValue(ImmutableList.of(TEST_LOI_MUTATION, TEST_SUBMISSION_MUTATION))
        val loi = localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
        var submission = localDataStore.getSubmission(loi, "submission id").blockingGet()
        assertEquivalent(TEST_SUBMISSION_MUTATION, submission)

        // now update the inserted submission with new responses
        val deltas = ImmutableList.of(
            ResponseDelta.builder()
                .setTaskId("task id")
                .setTaskType(Task.Type.TEXT)
                .setNewResponse(TextResponse.fromString("value for the really new task"))
                .build()
        )
        val mutation = TEST_SUBMISSION_MUTATION.toBuilder()
            .setResponseDeltas(deltas)
            .setId(2L)
            .setType(Mutation.Type.UPDATE)
            .build()
        localDataStore.applyAndEnqueue(mutation).test().assertComplete()
        localDataStore
            .getPendingMutations("loi id")
            .test()
            .assertValue(ImmutableList.of(TEST_LOI_MUTATION, TEST_SUBMISSION_MUTATION, mutation))

        // check if the submission was updated in the local database
        submission = localDataStore.getSubmission(loi, "submission id").blockingGet()
        assertEquivalent(mutation, submission)

        // also test that getSubmissions returns the same submission as well
        val submissions = localDataStore.getSubmissions(loi, "job id").blockingGet()
        assertThat(submissions).hasSize(1)
        assertEquivalent(mutation, submissions[0])
    }

    @Test
    fun testMergeSubmission() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait()
        val loi = localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
        val responseMap = ResponseMap.builder()
            .putResponse("task id", TextResponse.fromString("foo value").get())
            .build()
        val submission =
            localDataStore.getSubmission(loi, "submission id").blockingGet().toBuilder()
                .setResponses(responseMap)
                .build()
        localDataStore.mergeSubmission(submission).test().assertComplete()
        val responses =
            localDataStore.getSubmission(loi, submission.id).test().values()[0].responses
        assertThat("updated response")
            .isEqualTo(responses.getResponse("task id").get().toString())
    }

    @Test
    fun testDeleteSubmission() {
        // Add test submission
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait()
        val mutation =
            TEST_SUBMISSION_MUTATION.toBuilder().setId(null).setType(Mutation.Type.DELETE).build()

        // Calling applyAndEnqueue marks the local submission as deleted.
        localDataStore.applyAndEnqueue(mutation).blockingAwait()

        // Verify that local entity exists and its state is updated.
        submissionDao
            .findById("submission id")
            .test()
            .assertValue { submissionEntity: SubmissionEntity -> submissionEntity.state == EntityState.DELETED }

        // Verify that the local submission doesn't end up in getSubmissions().
        val loi = localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
        localDataStore.getSubmissions(loi, "task id").test().assertValue(ImmutableList.of())

        // After successful remote sync, delete submission is called by LocalMutationSyncWorker.
        localDataStore.deleteSubmission("submission id").blockingAwait()

        // Verify that the submission doesn't exist anymore
        localDataStore.getSubmission(loi, "submission id").test().assertNoValues()
    }

    @Test
    fun testDeleteLoi() {
        localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait()
        localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_LOI_MUTATION).blockingAwait()
        localDataStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait()
        val subscriber = localDataStore.getLocationsOfInterestOnceAndStream(TEST_SURVEY).test()

        // Assert that one LOI is streamed.
        val loi = localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet()
        subscriber.assertValueAt(0, ImmutableSet.of(loi))
        val mutation =
            TEST_LOI_MUTATION.toBuilder().setId(null).setType(Mutation.Type.DELETE).build()

        // Calling applyAndEnqueue marks the local LOI as deleted.
        localDataStore.applyAndEnqueue(mutation).blockingAwait()

        // Verify that local entity exists but its state is updated to DELETED.
        locationOfInterestDao
            .findById("loi id")
            .test()
            .assertValue { entity: LocationOfInterestEntity -> entity.state == EntityState.DELETED }

        // Verify that the local LOI is now removed from the latest LOI stream.
        subscriber.assertValueAt(1, ImmutableSet.of())

        // After successful remote sync, delete LOI is called by LocalMutationSyncWorker.
        localDataStore.deleteLocationOfInterest("loi id").blockingAwait()

        // Verify that the LOI doesn't exist anymore
        localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").test().assertNoValues()

        // Verify that the linked submission is also deleted.
        localDataStore.getSubmission(loi, "submission id").test().assertNoValues()
    }

    @Test
    fun testInsertTile() {
        localDataStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).test().assertComplete()
    }

    @Test
    fun testGetTile() {
        localDataStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait()
        localDataStore
            .getTileSet("some_url 1")
            .test()
            .assertValueCount(1)
            .assertValue(TEST_PENDING_TILE_SOURCE)
    }

    @Test
    fun testGetTilesOnceAndStream() {
        val subscriber = localDataStore.tileSetsOnceAndStream.test()
        subscriber.assertValue(ImmutableSet.of())
        localDataStore.insertOrUpdateTileSet(TEST_DOWNLOADED_TILE_SOURCE).blockingAwait()
        localDataStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait()
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
        localDataStore.insertOrUpdateTileSet(TEST_DOWNLOADED_TILE_SOURCE).blockingAwait()
        localDataStore.insertOrUpdateTileSet(TEST_FAILED_TILE_SOURCE).blockingAwait()
        localDataStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait()
        localDataStore
            .pendingTileSets
            .test()
            .assertValue(ImmutableList.of(TEST_PENDING_TILE_SOURCE))
    }

    @Test
    fun testInsertOfflineArea() {
        localDataStore.insertOrUpdateOfflineArea(TEST_OFFLINE_AREA).test().assertComplete()
    }

    @Test
    fun testGetOfflineAreas() {
        localDataStore.insertOrUpdateOfflineArea(TEST_OFFLINE_AREA).blockingAwait()
        localDataStore
            .offlineAreasOnceAndStream
            .test()
            .assertValue(ImmutableList.of(TEST_OFFLINE_AREA))
    }

    @Test
    fun testParseVertices_emptyString() {
        assertThat(LocationOfInterestEntity.parseVertices(""))
            .isEqualTo(ImmutableList.of<Any>())
    }

    @Test
    fun testFormatVertices_emptyList() {
        assertThat(LocationOfInterestEntity.formatVertices(ImmutableList.of())).isNull()
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
        private val TEST_JOB = Job(
            "job id",
            "heading title",
            ImmutableMap.builder<String, Task>().put(TEST_TASK.id, TEST_TASK).build()
        )
        private val TEST_SURVEY = Survey(
            "survey id",
            "survey 1",
            "foo description",
            ImmutableMap.builder<String, Job>().put(TEST_JOB.id, TEST_JOB).build()
        )
        private val TEST_POINT = Point(110.0, -23.1)
        private val TEST_POINT_2 = Point(51.0, 44.0)
        private val TEST_POLYGON_1 = ImmutableList.builder<Point>()
            .add(Point(49.874502, 8.655993))
            .add(Point(49.874099, 8.651173))
            .add(Point(49.872919, 8.651628))
            .add(Point(49.873164, 8.653515))
            .add(Point(49.874343, 8.653038))
            .add(Point(49.874502, 8.655993))
            .build()
        private val TEST_POLYGON_2 = ImmutableList.builder<Point>()
            .add(Point(49.865374, 8.646920))
            .add(Point(49.864241, 8.647286))
            .add(Point(49.864664, 8.650387))
            .add(Point(49.863102, 8.650445))
            .add(Point(49.863051, 8.647306))
            .add(Point(49.865374, 8.646920))
            .build()
        private val TEST_LOI_MUTATION = createTestLocationOfInterestMutation(TEST_POINT)
        private val TEST_POLYGON_LOI_MUTATION = createTestAreaOfInterestMutation(TEST_POLYGON_1)
        private val TEST_SUBMISSION_MUTATION = SubmissionMutation.builder()
            .setJob(TEST_JOB)
            .setSubmissionId("submission id")
            .setResponseDeltas(
                ImmutableList.of(
                    ResponseDelta.builder()
                        .setTaskId("task id")
                        .setTaskType(Task.Type.TEXT)
                        .setNewResponse(TextResponse.fromString("updated response"))
                        .build()
                )
            )
            .setId(1L)
            .setType(Mutation.Type.CREATE)
            .setSyncStatus(SyncStatus.PENDING)
            .setSurveyId("survey id")
            .setLocationOfInterestId("loi id")
            .setUserId("user id")
            .setClientTimestamp(Date())
            .build()
        private val TEST_PENDING_TILE_SOURCE =
            TileSet("some_url 1", "id_1", "some_path 1", TileSet.State.PENDING, 1)
        private val TEST_DOWNLOADED_TILE_SOURCE =
            TileSet("some_url 2", "id_2", "some_path 2", TileSet.State.DOWNLOADED, 1)
        private val TEST_FAILED_TILE_SOURCE =
            TileSet("some_url 3", "id_3", "some_path 3", TileSet.State.FAILED, 1)
        private val TEST_OFFLINE_AREA = OfflineArea(
            "id_1",
            OfflineArea.State.PENDING,
            LatLngBounds.builder().include(LatLng(0.0, 0.0)).build(),
            "Test Area"
        )

        private fun createTestLocationOfInterestMutation(point: Point): LocationOfInterestMutation {
            return LocationOfInterestMutation.builder()
                .setJobId("job id")
                .setLocation(Optional.ofNullable(point))
                .setPolygonVertices(ImmutableList.of())
                .setId(1L)
                .setLocationOfInterestId("loi id")
                .setType(Mutation.Type.CREATE)
                .setSyncStatus(SyncStatus.PENDING)
                .setUserId("user id")
                .setSurveyId("survey id")
                .setClientTimestamp(Date())
                .build()
        }

        private fun createTestAreaOfInterestMutation(
            polygonVertices: ImmutableList<Point>
        ): LocationOfInterestMutation {
            return LocationOfInterestMutation.builder()
                .setJobId("job id")
                .setLocation(Optional.empty())
                .setPolygonVertices(polygonVertices)
                .setId(1L)
                .setLocationOfInterestId("loi id")
                .setType(Mutation.Type.CREATE)
                .setSyncStatus(SyncStatus.PENDING)
                .setUserId("user id")
                .setSurveyId("survey id")
                .setClientTimestamp(Date())
                .build()
        }

        private fun assertEquivalent(mutation: SubmissionMutation, submission: Submission?) {
            assertThat(mutation.submissionId).isEqualTo(submission!!.id)
            assertThat(mutation.locationOfInterestId)
                .isEqualTo(submission.locationOfInterest.id)
            assertThat(mutation.job).isEqualTo(submission.job)
            assertThat(mutation.surveyId).isEqualTo(submission.surveyId)
            assertThat(mutation.userId).isEqualTo(submission.lastModified.user.id)
            assertThat(mutation.userId).isEqualTo(submission.created.user.id)
            MatcherAssert.assertThat(
                ResponseMap.builder().applyDeltas(mutation.responseDeltas).build(),
                Matchers.samePropertyValuesAs(submission.responses)
            )
        }
    }
}