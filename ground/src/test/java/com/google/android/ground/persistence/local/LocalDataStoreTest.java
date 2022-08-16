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

package com.google.android.ground.persistence.local;

import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.ground.BaseHiltTest;
import com.google.android.ground.model.Survey;
import com.google.android.ground.model.User;
import com.google.android.ground.model.basemap.OfflineArea;
import com.google.android.ground.model.basemap.tile.TileSet;
import com.google.android.ground.model.basemap.tile.TileSet.State;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.AreaOfInterest;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.locationofinterest.Point;
import com.google.android.ground.model.locationofinterest.PointOfInterest;
import com.google.android.ground.model.mutation.LocationOfInterestMutation;
import com.google.android.ground.model.mutation.Mutation;
import com.google.android.ground.model.mutation.Mutation.SyncStatus;
import com.google.android.ground.model.mutation.SubmissionMutation;
import com.google.android.ground.model.submission.ResponseDelta;
import com.google.android.ground.model.submission.ResponseMap;
import com.google.android.ground.model.submission.Submission;
import com.google.android.ground.model.submission.TextResponse;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.model.task.Task.Type;
import com.google.android.ground.persistence.local.room.dao.LocationOfInterestDao;
import com.google.android.ground.persistence.local.room.dao.SubmissionDao;
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestEntity;
import com.google.android.ground.persistence.local.room.models.EntityState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.subscribers.TestSubscriber;
import java.util.AbstractCollection;
import java.util.Date;
import java8.util.Optional;
import javax.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class LocalDataStoreTest extends BaseHiltTest {

  private static final User TEST_USER =
      new User("user1", "user@gmail.com", "user 1");

  private static final Task TEST_TASK =
      new Task("task id", 1, Type.TEXT_FIELD, "task label", false);

  private static final Job TEST_JOB =
      new Job("job id", "heading title",
          ImmutableMap.<String, Task>builder().put(TEST_TASK.getId(), TEST_TASK).build());

  private static final Survey TEST_SURVEY =
      Survey.newBuilder()
          .setId("survey id")
          .setTitle("survey 1")
          .setDescription("foo description")
          .putJob(TEST_JOB)
          .build();

  private static final Point TEST_POINT =
      Point.newBuilder().setLatitude(110.0).setLongitude(-23.1).build();

  private static final Point TEST_POINT_2 =
      Point.newBuilder().setLatitude(51.0).setLongitude(44.0).build();

  private static final ImmutableList<Point> TEST_POLYGON_1 =
      ImmutableList.<Point>builder()
          .add(Point.newBuilder().setLatitude(49.874502).setLongitude(8.655993).build())
          .add(Point.newBuilder().setLatitude(49.874099).setLongitude(8.651173).build())
          .add(Point.newBuilder().setLatitude(49.872919).setLongitude(8.651628).build())
          .add(Point.newBuilder().setLatitude(49.873164).setLongitude(8.653515).build())
          .add(Point.newBuilder().setLatitude(49.874343).setLongitude(8.653038).build())
          .build();

  private static final ImmutableList<Point> TEST_POLYGON_2 =
      ImmutableList.<Point>builder()
          .add(Point.newBuilder().setLatitude(49.865374).setLongitude(8.646920).build())
          .add(Point.newBuilder().setLatitude(49.864241).setLongitude(8.647286).build())
          .add(Point.newBuilder().setLatitude(49.864664).setLongitude(8.650387).build())
          .add(Point.newBuilder().setLatitude(49.863102).setLongitude(8.650445).build())
          .add(Point.newBuilder().setLatitude(49.863051).setLongitude(8.647306).build())
          .build();

  private static final LocationOfInterestMutation TEST_FEATURE_MUTATION =
      createTestLocationOfInterestMutation(TEST_POINT);

  private static final LocationOfInterestMutation TEST_POLYGON_FEATURE_MUTATION =
      createTestPolygonFeatureMutation(TEST_POLYGON_1);

  private static final SubmissionMutation TEST_SUBMISSION_MUTATION =
      SubmissionMutation.builder()
          .setJob(TEST_JOB)
          .setSubmissionId("submission id")
          .setResponseDeltas(
              ImmutableList.of(
                  ResponseDelta.builder()
                      .setTaskId("task id")
                      .setTaskType(Task.Type.TEXT_FIELD)
                      .setNewResponse(TextResponse.fromString("updated response"))
                      .build()))
          .setId(1L)
          .setType(Mutation.Type.CREATE)
          .setSyncStatus(SyncStatus.PENDING)
          .setSurveyId("survey id")
          .setLocationOfInterestId("loi id")
          .setUserId("user id")
          .setClientTimestamp(new Date())
          .build();

  private static final TileSet TEST_PENDING_TILE_SOURCE =
      TileSet.newBuilder()
          .setId("id_1")
          .setState(State.PENDING)
          .setPath("some_path 1")
          .setUrl("some_url 1")
          .setOfflineAreaReferenceCount(1)
          .build();

  private static final TileSet TEST_DOWNLOADED_TILE_SOURCE =
      TileSet.newBuilder()
          .setId("id_2")
          .setState(State.DOWNLOADED)
          .setPath("some_path 2")
          .setUrl("some_url 2")
          .setOfflineAreaReferenceCount(1)
          .build();

  private static final TileSet TEST_FAILED_TILE_SOURCE =
      TileSet.newBuilder()
          .setId("id_3")
          .setState(State.FAILED)
          .setPath("some_path 3")
          .setUrl("some_url 3")
          .setOfflineAreaReferenceCount(1)
          .build();

  private static final OfflineArea TEST_OFFLINE_AREA =
      OfflineArea.newBuilder()
          .setId("id_1")
          .setBounds(LatLngBounds.builder().include(new LatLng(0.0, 0.0)).build())
          .setState(OfflineArea.State.PENDING)
          .setName("Test Area")
          .build();

  @Inject
  LocalDataStore localDataStore;
  @Inject
  LocalValueStore localValueStore;
  @Inject
  SubmissionDao submissionDao;
  @Inject
  LocationOfInterestDao locationOfInterestDao;

  private static LocationOfInterestMutation createTestLocationOfInterestMutation(Point point) {
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
        .setClientTimestamp(new Date())
        .build();
  }

  private static LocationOfInterestMutation createTestPolygonFeatureMutation(
      ImmutableList<Point> polygonVertices) {
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
        .setClientTimestamp(new Date())
        .build();
  }

  private static void assertEquivalent(SubmissionMutation mutation, Submission submission) {
    assertThat(mutation.getSubmissionId()).isEqualTo(submission.getId());
    assertThat(mutation.getLocationOfInterestId())
        .isEqualTo(submission.getLocationOfInterest().getId());
    assertThat(mutation.getJob()).isEqualTo(submission.getJob());
    assertThat(mutation.getSurveyId()).isEqualTo(submission.getSurvey().getId());
    assertThat(mutation.getUserId()).isEqualTo(submission.getLastModified().getUser().getId());
    assertThat(mutation.getUserId()).isEqualTo(submission.getCreated().getUser().getId());
    MatcherAssert.assertThat(
        ResponseMap.builder().applyDeltas(mutation.getResponseDeltas()).build(),
        samePropertyValuesAs(submission.getResponses()));
  }

  @Test
  public void testInsertAndGetSurveys() {
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).test().assertComplete();
    localDataStore.getSurveys().test().assertValue(ImmutableList.of(TEST_SURVEY));
  }

  @Test
  public void testGetSurveyById() {
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();
    localDataStore.getSurveyById(TEST_SURVEY.getId()).test().assertValue(TEST_SURVEY);
  }

  @Test
  public void testDeleteSurvey() {
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();
    localDataStore.deleteSurvey(TEST_SURVEY).test().assertComplete();
    localDataStore.getSurveys().test().assertValue(AbstractCollection::isEmpty);
  }

  @Test
  public void testRemovedJobFromSurvey() {
    Job job1 = new Job("job 1", "job 1 name");
    Job job2 = new Job("job 2", "job 2 name");

    Survey survey =
        Survey.newBuilder()
            .setId("foo id")
            .setTitle("foo survey")
            .setDescription("foo survey description")
            .putJob(job1)
            .build();
    localDataStore.insertOrUpdateSurvey(survey).blockingAwait();

    survey =
        Survey.newBuilder()
            .setId("foo id")
            .setTitle("foo survey")
            .setDescription("foo survey description")
            .putJob(job2)
            .build();
    localDataStore.insertOrUpdateSurvey(survey).blockingAwait();

    localDataStore
        .getSurveyById("foo id")
        .test()
        .assertValue(result -> result.getJobs().equals(ImmutableList.of(job2)));
  }

  @Test
  public void testInsertAndGetUser() {
    localDataStore.insertOrUpdateUser(TEST_USER).test().assertComplete();
    localDataStore.getUser("user id").test().assertValue(TEST_USER);
  }

  @Test
  public void testApplyAndEnqueue_featureMutation() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();

    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).test().assertComplete();

    // assert that mutation is saved to local database
    localDataStore
        .getPendingMutations("loi id")
        .test()
        .assertValue(ImmutableList.of(TEST_FEATURE_MUTATION));

    localDataStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .test()
        .assertValue(feature -> ((PointOfInterest) feature).getPoint().equals(TEST_POINT));
  }

  @Test
  public void testApplyAndEnqueue_polygonFeatureMutation() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();

    localDataStore.applyAndEnqueue(TEST_POLYGON_FEATURE_MUTATION).test().assertComplete();

    // assert that mutation is saved to local database
    localDataStore
        .getPendingMutations("loi id")
        .test()
        .assertValue(ImmutableList.of(TEST_POLYGON_FEATURE_MUTATION));

    localDataStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .test()
        .assertValue(feature -> ((AreaOfInterest) feature).getVertices().equals(TEST_POLYGON_1));
  }

  @Test
  public void testGetFeaturesOnceAndStream() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();

    TestSubscriber<ImmutableSet<LocationOfInterest>> subscriber =
        localDataStore.getLocationsOfInterestOnceAndStream(TEST_SURVEY).test();

    subscriber.assertValue(ImmutableSet.of());

    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();

    PointOfInterest feature =
        (PointOfInterest) localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet();

    subscriber.assertValueSet(ImmutableSet.of(ImmutableSet.of(), ImmutableSet.of(feature)));
  }

  @Test
  public void testUpdateMutations() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();

    LocationOfInterestMutation mutation = createTestLocationOfInterestMutation(TEST_POINT_2);
    localDataStore.updateMutations(ImmutableList.of(mutation)).test().assertComplete();
    localDataStore
        .getPendingMutations(TEST_FEATURE_MUTATION.getLocationOfInterestId())
        .test()
        .assertValue(ImmutableList.of(mutation));
  }

  @Test
  public void testPolygonUpdateMutations() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_POLYGON_FEATURE_MUTATION).blockingAwait();

    LocationOfInterestMutation mutation = createTestPolygonFeatureMutation(TEST_POLYGON_2);
    localDataStore.updateMutations(ImmutableList.of(mutation)).test().assertComplete();
    localDataStore
        .getPendingMutations(TEST_POLYGON_FEATURE_MUTATION.getLocationOfInterestId())
        .test()
        .assertValue(ImmutableList.of(mutation));
  }

  @Test
  public void testFinalizePendingMutation() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();

    localDataStore
        .finalizePendingMutations(ImmutableList.of(TEST_FEATURE_MUTATION))
        .test()
        .assertComplete();

    localDataStore.getPendingMutations("loi id").test().assertValue(AbstractCollection::isEmpty);
  }

  @Test
  public void testMergeFeature() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();

    PointOfInterest feature =
        (PointOfInterest) localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet();
    feature = feature.toBuilder().setPoint(TEST_POINT_2).build();
    localDataStore.mergeLocationOfInterest(feature).test().assertComplete();

    localDataStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .test()
        .assertValue(newFeature -> ((PointOfInterest) newFeature).getPoint().equals(TEST_POINT_2));
  }

  @Test
  public void testMergePolygonFeature() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_POLYGON_FEATURE_MUTATION).blockingAwait();

    AreaOfInterest feature =
        (AreaOfInterest) localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet();
    feature = feature.toBuilder().setVertices(TEST_POLYGON_2).build();
    localDataStore.mergeLocationOfInterest(feature).test().assertComplete();

    localDataStore
        .getLocationOfInterest(TEST_SURVEY, "loi id")
        .test()
        .assertValue(
            newFeature -> ((AreaOfInterest) newFeature).getVertices().equals(TEST_POLYGON_2));
  }

  @Test
  public void testApplyAndEnqueue_submissionMutation() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();

    localDataStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).test().assertComplete();

    localDataStore
        .getPendingMutations("loi id")
        .test()
        .assertValue(ImmutableList.of(TEST_FEATURE_MUTATION, TEST_SUBMISSION_MUTATION));

    PointOfInterest feature =
        (PointOfInterest) localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet();
    Submission submission = localDataStore.getSubmission(feature, "submission id").blockingGet();
    assertEquivalent(TEST_SUBMISSION_MUTATION, submission);

    // now update the inserted submission with new responses
    ImmutableList<ResponseDelta> deltas =
        ImmutableList.of(
            ResponseDelta.builder()
                .setTaskId("task id")
                .setTaskType(Task.Type.TEXT_FIELD)
                .setNewResponse(TextResponse.fromString("value for the really new task"))
                .build());

    SubmissionMutation mutation =
        TEST_SUBMISSION_MUTATION.toBuilder()
            .setResponseDeltas(deltas)
            .setId(2L)
            .setType(Mutation.Type.UPDATE)
            .build();
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    localDataStore
        .getPendingMutations("loi id")
        .test()
        .assertValue(ImmutableList.of(TEST_FEATURE_MUTATION, TEST_SUBMISSION_MUTATION, mutation));

    // check if the submission was updated in the local database
    submission = localDataStore.getSubmission(feature, "submission id").blockingGet();
    assertEquivalent(mutation, submission);

    // also test that getSubmissions returns the same submission as well
    ImmutableList<Submission> submissions =
        localDataStore.getSubmissions(feature, "job id").blockingGet();
    assertThat(submissions).hasSize(1);
    assertEquivalent(mutation, submissions.get(0));
  }

  @Test
  public void testMergeSubmission() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait();
    PointOfInterest feature =
        (PointOfInterest) localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet();

    ResponseMap responseMap =
        ResponseMap.builder()
            .putResponse("task id", TextResponse.fromString("foo value").get())
            .build();

    Submission submission =
        localDataStore.getSubmission(feature, "submission id").blockingGet().toBuilder()
            .setResponses(responseMap)
            .build();

    localDataStore.mergeSubmission(submission).test().assertComplete();

    ResponseMap responses =
        localDataStore
            .getSubmission(feature, submission.getId())
            .test()
            .values()
            .get(0)
            .getResponses();
    assertThat("updated response").isEqualTo(responses.getResponse("task id").get().toString());
  }

  @Test
  public void testDeleteSubmission() {
    // Add test submission
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait();

    SubmissionMutation mutation =
        TEST_SUBMISSION_MUTATION.toBuilder().setId(null).setType(Mutation.Type.DELETE).build();

    // Calling applyAndEnqueue marks the local submission as deleted.
    localDataStore.applyAndEnqueue(mutation).blockingAwait();

    // Verify that local entity exists and its state is updated.
    submissionDao
        .findById("submission id")
        .test()
        .assertValue(submissionEntity -> submissionEntity.getState() == EntityState.DELETED);

    // Verify that the local submission doesn't end up in getSubmissions().
    PointOfInterest feature =
        (PointOfInterest) localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet();
    localDataStore.getSubmissions(feature, "task id").test().assertValue(ImmutableList.of());

    // After successful remote sync, delete submission is called by LocalMutationSyncWorker.
    localDataStore.deleteSubmission("submission id").blockingAwait();

    // Verify that the submission doesn't exist anymore
    localDataStore.getSubmission(feature, "submission id").test().assertNoValues();
  }

  @Test
  public void testDeleteFeature() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateSurvey(TEST_SURVEY).blockingAwait();

    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_SUBMISSION_MUTATION).blockingAwait();

    TestSubscriber<ImmutableSet<LocationOfInterest>> subscriber =
        localDataStore.getLocationsOfInterestOnceAndStream(TEST_SURVEY).test();

    // Assert that one feature is streamed.
    PointOfInterest feature =
        (PointOfInterest) localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").blockingGet();
    subscriber.assertValueAt(0, ImmutableSet.of(feature));

    LocationOfInterestMutation mutation =
        TEST_FEATURE_MUTATION.toBuilder().setId(null).setType(Mutation.Type.DELETE).build();

    // Calling applyAndEnqueue marks the local feature as deleted.
    localDataStore.applyAndEnqueue(mutation).blockingAwait();

    // Verify that local entity exists but its state is updated to DELETED.
    locationOfInterestDao
        .findById("loi id")
        .test()
        .assertValue(featureEntity -> featureEntity.getState() == EntityState.DELETED);

    // Verify that the local feature is now removed from the latest feature stream.
    subscriber.assertValueAt(1, ImmutableSet.of());

    // After successful remote sync, delete feature is called by LocalMutationSyncWorker.
    localDataStore.deleteLocationOfInterest("loi id").blockingAwait();

    // Verify that the feature doesn't exist anymore
    localDataStore.getLocationOfInterest(TEST_SURVEY, "loi id").test().assertNoValues();

    // Verify that the linked submission is also deleted.
    localDataStore.getSubmission(feature, "submission id").test().assertNoValues();
  }

  @Test
  public void testInsertTile() {
    localDataStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).test().assertComplete();
  }

  @Test
  public void testGetTile() {
    localDataStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait();
    localDataStore
        .getTileSet("some_url 1")
        .test()
        .assertValueCount(1)
        .assertValue(TEST_PENDING_TILE_SOURCE);
  }

  @Test
  public void testGetTilesOnceAndStream() {
    TestSubscriber<ImmutableSet<TileSet>> subscriber =
        localDataStore.getTileSetsOnceAndStream().test();

    subscriber.assertValue(ImmutableSet.of());

    localDataStore.insertOrUpdateTileSet(TEST_DOWNLOADED_TILE_SOURCE).blockingAwait();
    localDataStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait();

    subscriber.assertValueSet(
        ImmutableSet.of(
            ImmutableSet.of(),
            ImmutableSet.of(TEST_DOWNLOADED_TILE_SOURCE),
            ImmutableSet.of(TEST_DOWNLOADED_TILE_SOURCE, TEST_PENDING_TILE_SOURCE)));
  }

  @Test
  public void testGetPendingTile() {
    localDataStore.insertOrUpdateTileSet(TEST_DOWNLOADED_TILE_SOURCE).blockingAwait();
    localDataStore.insertOrUpdateTileSet(TEST_FAILED_TILE_SOURCE).blockingAwait();
    localDataStore.insertOrUpdateTileSet(TEST_PENDING_TILE_SOURCE).blockingAwait();
    localDataStore
        .getPendingTileSets()
        .test()
        .assertValue(ImmutableList.of(TEST_PENDING_TILE_SOURCE));
  }

  @Test
  public void testInsertOfflineArea() {
    localDataStore.insertOrUpdateOfflineArea(TEST_OFFLINE_AREA).test().assertComplete();
  }

  @Test
  public void testGetOfflineAreas() {
    localDataStore.insertOrUpdateOfflineArea(TEST_OFFLINE_AREA).blockingAwait();
    localDataStore
        .getOfflineAreasOnceAndStream()
        .test()
        .assertValue(ImmutableList.of(TEST_OFFLINE_AREA));
  }

  @Test
  public void testParseVertices_emptyString() {
    assertThat(LocationOfInterestEntity.parseVertices("")).isEqualTo(ImmutableList.of());
  }

  @Test
  public void testFormatVertices_emptyList() {
    assertThat(LocationOfInterestEntity.formatVertices(ImmutableList.of())).isNull();
  }

  @Test
  public void testTermsOfServiceAccepted() {
    localValueStore.setTermsOfServiceAccepted(true);
    assertThat(localValueStore.isTermsOfServiceAccepted()).isTrue();
  }

  @Test
  public void testTermsOfServiceNotAccepted() {
    assertThat(localValueStore.isTermsOfServiceAccepted()).isFalse();
  }
}
