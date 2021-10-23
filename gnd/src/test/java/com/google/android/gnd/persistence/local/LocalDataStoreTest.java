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

package com.google.android.gnd.persistence.local;

import static com.google.common.truth.Truth.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.HiltTestWithRobolectricRunner;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Mutation.SyncStatus;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.basemap.OfflineBaseMap;
import com.google.android.gnd.model.basemap.tile.TileSource;
import com.google.android.gnd.model.basemap.tile.TileSource.State;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.android.gnd.persistence.local.room.dao.FeatureDao;
import com.google.android.gnd.persistence.local.room.dao.ObservationDao;
import com.google.android.gnd.persistence.local.room.entity.FeatureEntity;
import com.google.android.gnd.persistence.local.room.models.EntityState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.subscribers.TestSubscriber;
import java.util.AbstractCollection;
import java.util.Date;
import java8.util.Optional;
import javax.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

@HiltAndroidTest
public class LocalDataStoreTest extends HiltTestWithRobolectricRunner {

  private static final User TEST_USER =
      User.builder().setId("user id").setEmail("user@gmail.com").setDisplayName("user 1").build();

  private static final Field TEST_FIELD =
      Field.newBuilder()
          .setId("field id")
          .setIndex(1)
          .setLabel("field label")
          .setRequired(false)
          .setType(Field.Type.TEXT_FIELD)
          .build();

  private static final Form TEST_FORM =
      Form.newBuilder()
          .setId("form id")
          .setElements(ImmutableList.of(Element.ofField(TEST_FIELD)))
          .build();

  private static final Layer TEST_LAYER =
      Layer.newBuilder()
          .setId("layer id")
          .setName("heading title")
          .setDefaultStyle(Style.builder().setColor("000").build())
          .setForm(TEST_FORM)
          .build();

  private static final Project TEST_PROJECT =
      Project.newBuilder()
          .setId("project id")
          .setTitle("project 1")
          .setDescription("foo description")
          .putLayer("layer id", TEST_LAYER)
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

  private static final FeatureMutation TEST_FEATURE_MUTATION =
      createTestFeatureMutation(TEST_POINT);

  private static final FeatureMutation TEST_POLYGON_FEATURE_MUTATION =
      createTestPolygonFeatureMutation(TEST_POLYGON_1);

  private static final ObservationMutation TEST_OBSERVATION_MUTATION =
      ObservationMutation.builder()
          .setId(1L)
          .setObservationId("observation id")
          .setType(Mutation.Type.CREATE)
          .setSyncStatus(SyncStatus.PENDING)
          .setProjectId("project id")
          .setFeatureId("feature id")
          .setLayerId("layer id")
          .setForm(TEST_FORM)
          .setUserId("user id")
          .setResponseDeltas(
              ImmutableList.of(
                  ResponseDelta.builder()
                      .setFieldId("field id")
                      .setFieldType(Field.Type.TEXT_FIELD)
                      .setNewResponse(TextResponse.fromString("updated response"))
                      .build()))
          .setClientTimestamp(new Date())
          .build();

  private static final TileSource TEST_PENDING_TILE_SOURCE =
      TileSource.newBuilder()
          .setId("id_1")
          .setState(State.PENDING)
          .setPath("some_path 1")
          .setUrl("some_url 1")
          .setBasemapReferenceCount(1)
          .build();

  private static final TileSource TEST_DOWNLOADED_TILE_SOURCE =
      TileSource.newBuilder()
          .setId("id_2")
          .setState(State.DOWNLOADED)
          .setPath("some_path 2")
          .setUrl("some_url 2")
          .setBasemapReferenceCount(1)
          .build();

  private static final TileSource TEST_FAILED_TILE_SOURCE =
      TileSource.newBuilder()
          .setId("id_3")
          .setState(State.FAILED)
          .setPath("some_path 3")
          .setUrl("some_url 3")
          .setBasemapReferenceCount(1)
          .build();

  private static final OfflineBaseMap TEST_OFFLINE_AREA =
      OfflineBaseMap.newBuilder()
          .setId("id_1")
          .setBounds(LatLngBounds.builder().include(new LatLng(0.0, 0.0)).build())
          .setState(OfflineBaseMap.State.PENDING)
          .setName("Test Area")
          .build();

  @Inject LocalDataStore localDataStore;
  @Inject LocalValueStore localValueStore;
  @Inject ObservationDao observationDao;
  @Inject FeatureDao featureDao;

  private static FeatureMutation createTestFeatureMutation(Point point) {
    return FeatureMutation.builder()
        .setId(1L)
        .setFeatureId("feature id")
        .setType(Mutation.Type.CREATE)
        .setSyncStatus(SyncStatus.PENDING)
        .setUserId("user id")
        .setProjectId("project id")
        .setLayerId("layer id")
        .setNewLocation(Optional.ofNullable(point))
        .setNewPolygonVertices(ImmutableList.of())
        .setClientTimestamp(new Date())
        .build();
  }

  private static FeatureMutation createTestPolygonFeatureMutation(
      ImmutableList<Point> polygonVertices) {
    return FeatureMutation.builder()
        .setId(1L)
        .setFeatureId("feature id")
        .setType(Mutation.Type.CREATE)
        .setSyncStatus(SyncStatus.PENDING)
        .setUserId("user id")
        .setProjectId("project id")
        .setLayerId("layer id")
        .setNewLocation(Optional.empty())
        .setNewPolygonVertices(polygonVertices)
        .setClientTimestamp(new Date())
        .build();
  }

  private static void assertEquivalent(ObservationMutation mutation, Observation observation) {
    assertThat(mutation.getObservationId()).isEqualTo(observation.getId());
    assertThat(mutation.getFeatureId()).isEqualTo(observation.getFeature().getId());
    assertThat(mutation.getForm()).isEqualTo(observation.getForm());
    assertThat(mutation.getProjectId()).isEqualTo(observation.getProject().getId());
    assertThat(mutation.getUserId()).isEqualTo(observation.getLastModified().getUser().getId());
    assertThat(mutation.getUserId()).isEqualTo(observation.getCreated().getUser().getId());
    MatcherAssert.assertThat(
        ResponseMap.builder().applyDeltas(mutation.getResponseDeltas()).build(),
        samePropertyValuesAs(observation.getResponses()));
  }

  @Test
  public void testInsertAndGetProjects() {
    localDataStore.insertOrUpdateProject(TEST_PROJECT).test().assertComplete();
    localDataStore.getProjects().test().assertValue(ImmutableList.of(TEST_PROJECT));
  }

  @Test
  public void testGetProjectById() {
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();
    localDataStore.getProjectById("project id").test().assertValue(TEST_PROJECT);
  }

  @Test
  public void testDeleteProject() {
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();
    localDataStore.deleteProject(TEST_PROJECT).test().assertComplete();
    localDataStore.getProjects().test().assertValue(AbstractCollection::isEmpty);
  }

  @Test
  public void testRemovedLayerFromProject() {
    Layer layer1 =
        Layer.newBuilder()
            .setId("layer 1")
            .setName("layer 1 name")
            .setDefaultStyle(Style.builder().setColor("000").build())
            .build();
    Layer layer2 =
        Layer.newBuilder()
            .setId("layer 2")
            .setName("layer 2 name")
            .setDefaultStyle(Style.builder().setColor("000").build())
            .build();

    Project project =
        Project.newBuilder()
            .setId("foo id")
            .setTitle("foo project")
            .setDescription("foo project description")
            .putLayer(layer1.getId(), layer1)
            .build();
    localDataStore.insertOrUpdateProject(project).blockingAwait();

    project =
        Project.newBuilder()
            .setId("foo id")
            .setTitle("foo project")
            .setDescription("foo project description")
            .putLayer(layer2.getId(), layer2)
            .build();
    localDataStore.insertOrUpdateProject(project).blockingAwait();

    localDataStore
        .getProjectById("foo id")
        .test()
        .assertValue(result -> result.getLayers().equals(ImmutableList.of(layer2)));
  }

  @Test
  public void testInsertAndGetUser() {
    localDataStore.insertOrUpdateUser(TEST_USER).test().assertComplete();
    localDataStore.getUser("user id").test().assertValue(TEST_USER);
  }

  @Test
  public void testApplyAndEnqueue_featureMutation() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();

    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).test().assertComplete();

    // assert that mutation is saved to local database
    localDataStore
        .getPendingMutations("feature id")
        .test()
        .assertValue(ImmutableList.of(TEST_FEATURE_MUTATION));

    localDataStore
        .getFeature(TEST_PROJECT, "feature id")
        .test()
        .assertValue(feature -> ((PointFeature) feature).getPoint().equals(TEST_POINT));
  }

  @Test
  public void testApplyAndEnqueue_polygonFeatureMutation() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();

    localDataStore.applyAndEnqueue(TEST_POLYGON_FEATURE_MUTATION).test().assertComplete();

    // assert that mutation is saved to local database
    localDataStore
        .getPendingMutations("feature id")
        .test()
        .assertValue(ImmutableList.of(TEST_POLYGON_FEATURE_MUTATION));

    localDataStore
        .getFeature(TEST_PROJECT, "feature id")
        .test()
        .assertValue(feature -> ((PolygonFeature) feature).getVertices().equals(TEST_POLYGON_1));
  }

  @Test
  public void testGetFeaturesOnceAndStream() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();

    TestSubscriber<ImmutableSet<Feature>> subscriber =
        localDataStore.getFeaturesOnceAndStream(TEST_PROJECT).test();

    subscriber.assertValue(ImmutableSet.of());

    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();

    PointFeature feature =
        (PointFeature) localDataStore.getFeature(TEST_PROJECT, "feature id").blockingGet();

    subscriber.assertValueSet(ImmutableSet.of(ImmutableSet.of(), ImmutableSet.of(feature)));
  }

  @Test
  public void testUpdateMutations() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();

    FeatureMutation mutation = createTestFeatureMutation(TEST_POINT_2);
    localDataStore.updateMutations(ImmutableList.of(mutation)).test().assertComplete();
    localDataStore
        .getPendingMutations(TEST_FEATURE_MUTATION.getFeatureId())
        .test()
        .assertValue(ImmutableList.of(mutation));
  }

  @Test
  public void testPolygonUpdateMutations() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_POLYGON_FEATURE_MUTATION).blockingAwait();

    FeatureMutation mutation = createTestPolygonFeatureMutation(TEST_POLYGON_2);
    localDataStore.updateMutations(ImmutableList.of(mutation)).test().assertComplete();
    localDataStore
        .getPendingMutations(TEST_POLYGON_FEATURE_MUTATION.getFeatureId())
        .test()
        .assertValue(ImmutableList.of(mutation));
  }

  @Test
  public void testFinalizePendingMutation() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();

    localDataStore
        .finalizePendingMutations(ImmutableList.of(TEST_FEATURE_MUTATION))
        .test()
        .assertComplete();

    localDataStore
        .getPendingMutations("feature id")
        .test()
        .assertValue(AbstractCollection::isEmpty);
  }

  @Test
  public void testMergeFeature() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();

    PointFeature feature =
        (PointFeature) localDataStore.getFeature(TEST_PROJECT, "feature id").blockingGet();
    feature = feature.toBuilder().setPoint(TEST_POINT_2).build();
    localDataStore.mergeFeature(feature).test().assertComplete();

    localDataStore
        .getFeature(TEST_PROJECT, "feature id")
        .test()
        .assertValue(newFeature -> ((PointFeature) newFeature).getPoint().equals(TEST_POINT_2));
  }

  @Test
  public void testMergePolygonFeature() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_POLYGON_FEATURE_MUTATION).blockingAwait();

    PolygonFeature feature =
        (PolygonFeature) localDataStore.getFeature(TEST_PROJECT, "feature id").blockingGet();
    feature = feature.toBuilder().setVertices(TEST_POLYGON_2).build();
    localDataStore.mergeFeature(feature).test().assertComplete();

    localDataStore
        .getFeature(TEST_PROJECT, "feature id")
        .test()
        .assertValue(
            newFeature -> ((PolygonFeature) newFeature).getVertices().equals(TEST_POLYGON_2));
  }

  @Test
  public void testApplyAndEnqueue_observationMutation() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();

    localDataStore.applyAndEnqueue(TEST_OBSERVATION_MUTATION).test().assertComplete();

    localDataStore
        .getPendingMutations("feature id")
        .test()
        .assertValue(ImmutableList.of(TEST_FEATURE_MUTATION, TEST_OBSERVATION_MUTATION));

    PointFeature feature =
        (PointFeature) localDataStore.getFeature(TEST_PROJECT, "feature id").blockingGet();
    Observation observation =
        localDataStore.getObservation(feature, "observation id").blockingGet();
    assertEquivalent(TEST_OBSERVATION_MUTATION, observation);

    // now update the inserted observation with new responses
    ImmutableList<ResponseDelta> deltas =
        ImmutableList.of(
            ResponseDelta.builder()
                .setFieldId("field id")
                .setFieldType(Field.Type.TEXT_FIELD)
                .setNewResponse(TextResponse.fromString("value for the really new field"))
                .build());

    ObservationMutation mutation =
        TEST_OBSERVATION_MUTATION.toBuilder()
            .setId(2L)
            .setResponseDeltas(deltas)
            .setType(Mutation.Type.UPDATE)
            .build();
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    localDataStore
        .getPendingMutations("feature id")
        .test()
        .assertValue(ImmutableList.of(TEST_FEATURE_MUTATION, TEST_OBSERVATION_MUTATION, mutation));

    // check if the observation was updated in the local database
    observation = localDataStore.getObservation(feature, "observation id").blockingGet();
    assertEquivalent(mutation, observation);

    // also test that getObservations returns the same observation as well
    ImmutableList<Observation> observations =
        localDataStore.getObservations(feature, "form id").blockingGet();
    assertThat(observations).hasSize(1);
    assertEquivalent(mutation, observations.get(0));
  }

  @Test
  public void testMergeObservation() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_OBSERVATION_MUTATION).blockingAwait();
    PointFeature feature =
        (PointFeature) localDataStore.getFeature(TEST_PROJECT, "feature id").blockingGet();

    ResponseMap responseMap =
        ResponseMap.builder()
            .putResponse("field id", TextResponse.fromString("foo value").get())
            .build();

    Observation observation =
        localDataStore.getObservation(feature, "observation id").blockingGet().toBuilder()
            .setResponses(responseMap)
            .build();

    localDataStore.mergeObservation(observation).test().assertComplete();

    ResponseMap responses =
        localDataStore
            .getObservation(feature, observation.getId())
            .test()
            .values()
            .get(0)
            .getResponses();
    assertThat("updated response").isEqualTo(responses.getResponse("field id").get().toString());
  }

  @Test
  public void testDeleteObservation() {
    // Add test observation
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_OBSERVATION_MUTATION).blockingAwait();

    ObservationMutation mutation =
        TEST_OBSERVATION_MUTATION.toBuilder().setId(null).setType(Mutation.Type.DELETE).build();

    // Calling applyAndEnqueue marks the local observation as deleted.
    localDataStore.applyAndEnqueue(mutation).blockingAwait();

    // Verify that local entity exists and its state is updated.
    observationDao
        .findById("observation id")
        .test()
        .assertValue(observationEntity -> observationEntity.getState() == EntityState.DELETED);

    // Verify that the local observation doesn't end up in getObservations().
    PointFeature feature =
        (PointFeature) localDataStore.getFeature(TEST_PROJECT, "feature id").blockingGet();
    localDataStore.getObservations(feature, "form id").test().assertValue(ImmutableList.of());

    // After successful remote sync, delete observation is called by LocalMutationSyncWorker.
    localDataStore.deleteObservation("observation id").blockingAwait();

    // Verify that the observation doesn't exist anymore
    localDataStore.getObservation(feature, "observation id").test().assertNoValues();
  }

  @Test
  public void testDeleteFeature() {
    localDataStore.insertOrUpdateUser(TEST_USER).blockingAwait();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).blockingAwait();

    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).blockingAwait();
    localDataStore.applyAndEnqueue(TEST_OBSERVATION_MUTATION).blockingAwait();

    TestSubscriber<ImmutableSet<Feature>> subscriber =
        localDataStore.getFeaturesOnceAndStream(TEST_PROJECT).test();

    // Assert that one feature is streamed.
    PointFeature feature =
        (PointFeature) localDataStore.getFeature(TEST_PROJECT, "feature id").blockingGet();
    subscriber.assertValueAt(0, ImmutableSet.of(feature));

    FeatureMutation mutation =
        TEST_FEATURE_MUTATION.toBuilder().setId(null).setType(Mutation.Type.DELETE).build();

    // Calling applyAndEnqueue marks the local feature as deleted.
    localDataStore.applyAndEnqueue(mutation).blockingAwait();

    // Verify that local entity exists but its state is updated to DELETED.
    featureDao
        .findById("feature id")
        .test()
        .assertValue(featureEntity -> featureEntity.getState() == EntityState.DELETED);

    // Verify that the local feature is now removed from the latest feature stream.
    subscriber.assertValueAt(1, ImmutableSet.of());

    // After successful remote sync, delete feature is called by LocalMutationSyncWorker.
    localDataStore.deleteFeature("feature id").blockingAwait();

    // Verify that the feature doesn't exist anymore
    localDataStore.getFeature(TEST_PROJECT, "feature id").test().assertNoValues();

    // Verify that the linked observation is also deleted.
    localDataStore.getObservation(feature, "observation id").test().assertNoValues();
  }

  @Test
  public void testInsertTile() {
    localDataStore.insertOrUpdateTileSource(TEST_PENDING_TILE_SOURCE).test().assertComplete();
  }

  @Test
  public void testGetTile() {
    localDataStore.insertOrUpdateTileSource(TEST_PENDING_TILE_SOURCE).blockingAwait();
    localDataStore
        .getTileSource("some_url 1")
        .test()
        .assertValueCount(1)
        .assertValue(TEST_PENDING_TILE_SOURCE);
  }

  @Test
  public void testGetTilesOnceAndStream() {
    TestSubscriber<ImmutableSet<TileSource>> subscriber =
        localDataStore.getTileSourcesOnceAndStream().test();

    subscriber.assertValue(ImmutableSet.of());

    localDataStore.insertOrUpdateTileSource(TEST_DOWNLOADED_TILE_SOURCE).blockingAwait();
    localDataStore.insertOrUpdateTileSource(TEST_PENDING_TILE_SOURCE).blockingAwait();

    subscriber.assertValueSet(
        ImmutableSet.of(
            ImmutableSet.of(),
            ImmutableSet.of(TEST_DOWNLOADED_TILE_SOURCE),
            ImmutableSet.of(TEST_DOWNLOADED_TILE_SOURCE, TEST_PENDING_TILE_SOURCE)));
  }

  @Test
  public void testGetPendingTile() {
    localDataStore.insertOrUpdateTileSource(TEST_DOWNLOADED_TILE_SOURCE).blockingAwait();
    localDataStore.insertOrUpdateTileSource(TEST_FAILED_TILE_SOURCE).blockingAwait();
    localDataStore.insertOrUpdateTileSource(TEST_PENDING_TILE_SOURCE).blockingAwait();
    localDataStore
        .getPendingTileSources()
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
    assertThat(FeatureEntity.parseVertices("")).isEqualTo(ImmutableList.of());
  }

  @Test
  public void testFormatVertices_emptyList() {
    assertThat(FeatureEntity.formatVertices(ImmutableList.of())).isNull();
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
