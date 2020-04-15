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

import static org.hamcrest.Matchers.samePropertyValuesAs;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gnd.TestApplication;
import com.google.android.gnd.inject.DaggerTestComponent;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.model.basemap.tile.Tile.State;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.form.MultipleChoice;
import com.google.android.gnd.model.form.MultipleChoice.Cardinality;
import com.google.android.gnd.model.form.Option;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.model.observation.TextResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.reactivex.subscribers.TestSubscriber;
import java.util.AbstractCollection;
import java.util.Date;
import java8.util.Optional;
import javax.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class LocalDataStoreTest {

  private static final User FAKE_USER =
      User.builder()
          .setId("test_user_id")
          .setEmail("test@gmail.com")
          .setDisplayName("test user")
          .build();

  private static final MultipleChoice FAKE_MULTIPLE_CHOICE =
      MultipleChoice.newBuilder()
          .setCardinality(Cardinality.SELECT_ONE)
          .setOptions(
              ImmutableList.of(
                  Option.newBuilder().setCode("a").setLabel("Name").build(),
                  Option.newBuilder().setCode("b").setLabel("Age").build()))
          .build();

  private static final Field FAKE_FIELD =
      Field.newBuilder()
          .setId("field id")
          .setLabel("field label")
          .setRequired(false)
          .setType(Type.MULTIPLE_CHOICE)
          .setMultipleChoice(FAKE_MULTIPLE_CHOICE)
          .build();

  private static final Form FAKE_FORM =
      Form.newBuilder()
          .setId("form id")
          .setElements(ImmutableList.of(Element.ofField(FAKE_FIELD)))
          .build();

  private static final Layer FAKE_LAYER =
      Layer.newBuilder()
          .setId("layer id")
          .setItemLabel("item label")
          .setListHeading("heading title")
          .setDefaultStyle(Style.builder().setColor("000").build())
          .setForm(FAKE_FORM)
          .build();

  private static final Project FAKE_PROJECT =
      Project.newBuilder()
          .setId("project id")
          .setTitle("project 1")
          .setDescription("foo description")
          .putLayer(FAKE_LAYER.getId(), FAKE_LAYER)
          .build();

  private static final FeatureMutation FAKE_FEATURE_MUTATION =
      FeatureMutation.builder()
          .setId(1L)
          .setFeatureId("feature id")
          .setType(Mutation.Type.CREATE)
          .setUserId(FAKE_USER.getId())
          .setProjectId(FAKE_PROJECT.getId())
          .setLayerId(FAKE_LAYER.getId())
          .setNewLocation(
              Optional.ofNullable(
                  Point.newBuilder().setLatitude(110.0).setLongitude(-23.1).build()))
          .setClientTimestamp(new Date())
          .build();

  private static final ObservationMutation FAKE_OBSERVATION_MUTATION =
      ObservationMutation.builder()
          .setObservationId("observation id")
          .setType(Mutation.Type.CREATE)
          .setProjectId(FAKE_PROJECT.getId())
          .setFeatureId(FAKE_FEATURE_MUTATION.getFeatureId())
          .setLayerId(FAKE_LAYER.getId())
          .setFormId(FAKE_FORM.getId())
          .setUserId(FAKE_USER.getId())
          .setResponseDeltas(
              ImmutableList.of(
                  ResponseDelta.builder()
                      .setFieldId("field id")
                      .setNewResponse(TextResponse.fromString("response for field id"))
                      .build()))
          .setClientTimestamp(new Date())
          .build();

  // This rule makes sure that Room executes all the database operations instantly.
  @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @Inject LocalDataStore localDataStore;

  private static void assertObservationMutation(
      ObservationMutation expected, ObservationMutation actual) {
    Assert.assertEquals(expected.getResponseDeltas(), actual.getResponseDeltas());
    Assert.assertEquals(expected.getType(), actual.getType());
    Assert.assertEquals(expected.getUserId(), actual.getUserId());
    Assert.assertEquals(expected.getProjectId(), actual.getProjectId());
    Assert.assertEquals(expected.getFeatureId(), actual.getFeatureId());
    Assert.assertEquals(expected.getLayerId(), actual.getLayerId());
    Assert.assertEquals(expected.getClientTimestamp(), actual.getClientTimestamp());
    Assert.assertEquals(0, actual.getRetryCount());
    Assert.assertNull(actual.getLastError());
  }

  @Before
  public void setUp() {
    DaggerTestComponent.create().inject(this);
  }

  @Test
  public void testGetProjects() {
    Project project1 =
        Project.newBuilder()
            .setId("id 1")
            .setTitle("project 1")
            .setDescription("foo description")
            .build();
    Project project2 =
        Project.newBuilder()
            .setId("id 2")
            .setTitle("project 2")
            .setDescription("foo description 2")
            .build();
    localDataStore.insertOrUpdateProject(project1).subscribe();
    localDataStore.insertOrUpdateProject(project2).subscribe();
    localDataStore.getProjects().test().assertValue(ImmutableList.of(project1, project2));
  }

  @Test
  public void testGetProjectById() {
    Project project = FAKE_PROJECT;
    localDataStore.insertOrUpdateProject(project).subscribe();
    localDataStore.getProjectById(project.getId()).test().assertValue(project);
  }

  @Test
  public void testDeleteProject() {
    Project project = FAKE_PROJECT;
    localDataStore.insertOrUpdateProject(project).subscribe();
    localDataStore.deleteProject(project).test().assertComplete();
    localDataStore.getProjects().test().assertValue(AbstractCollection::isEmpty);
  }

  @Test
  public void testGetUser() {
    User user = FAKE_USER;
    localDataStore.insertOrUpdateUser(user).subscribe();
    localDataStore.getUser(user.getId()).test().assertValue(user);
  }

  @Test
  public void testApplyAndEnqueue_featureMutation() {
    User user = FAKE_USER;
    localDataStore.insertOrUpdateUser(user).subscribe();

    Project project = FAKE_PROJECT;
    localDataStore.insertOrUpdateProject(project).subscribe();

    Layer layer = project.getLayers().get(0);
    FeatureMutation mutation = FAKE_FEATURE_MUTATION;
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    // assert that mutation is saved to local database
    localDataStore
        .getPendingMutations(mutation.getFeatureId())
        .test()
        .assertValue(ImmutableList.of(mutation));

    // assert feature is saved to local database
    Feature feature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(mutation.getFeatureId(), feature.getId());
    Assert.assertEquals(project, feature.getProject());
    Assert.assertEquals(layer.getItemLabel(), feature.getTitle());
    Assert.assertEquals(layer, feature.getLayer());
    Assert.assertNull(feature.getCustomId());
    Assert.assertNull(feature.getCaption());
    Assert.assertEquals(mutation.getNewLocation().get(), feature.getPoint());
    Assert.assertEquals(user, feature.getCreated().getUser());
    Assert.assertEquals(user, feature.getLastModified().getUser());
  }

  @Test
  public void testGetFeaturesOnceAndStream() {
    localDataStore.insertOrUpdateUser(FAKE_USER).subscribe();

    Project project = FAKE_PROJECT;
    localDataStore.insertOrUpdateProject(project).subscribe();

    TestSubscriber<ImmutableSet<Feature>> subscriber =
        localDataStore.getFeaturesOnceAndStream(project).test();

    subscriber.assertValueCount(1);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);

    FeatureMutation mutation = FAKE_FEATURE_MUTATION;
    localDataStore.applyAndEnqueue(mutation).subscribe();

    Feature feature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();

    subscriber.assertValueCount(2);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);
    subscriber.assertValueAt(1, ImmutableSet.of(feature));
  }

  @Test
  public void testUpdateMutations() {
    localDataStore.insertOrUpdateUser(FAKE_USER).subscribe();
    localDataStore.insertOrUpdateProject(FAKE_PROJECT).subscribe();

    FeatureMutation mutation = FAKE_FEATURE_MUTATION;
    localDataStore.applyAndEnqueue(mutation).subscribe();

    Point newPoint = Point.newBuilder().setLatitude(51.0).setLongitude(44.0).build();
    Mutation updatedMutation =
        mutation.toBuilder().setNewLocation(Optional.ofNullable(newPoint)).build();

    localDataStore.updateMutations(ImmutableList.of(updatedMutation)).test().assertComplete();

    ImmutableList<Mutation> savedMutations =
        localDataStore.getPendingMutations(updatedMutation.getFeatureId()).blockingGet();
    Assert.assertEquals(1, savedMutations.size());

    FeatureMutation savedMutation = (FeatureMutation) savedMutations.get(0);
    Assert.assertEquals(newPoint, savedMutation.getNewLocation().get());
  }

  @Test
  public void testRemovePendingMutation() {
    localDataStore.insertOrUpdateUser(FAKE_USER).subscribe();
    localDataStore.insertOrUpdateProject(FAKE_PROJECT).subscribe();

    FeatureMutation mutation = FAKE_FEATURE_MUTATION;
    localDataStore.applyAndEnqueue(mutation).subscribe();

    localDataStore.removePendingMutations(ImmutableList.of(mutation)).test().assertComplete();

    localDataStore
        .getPendingMutations(mutation.getFeatureId())
        .test()
        .assertValue(AbstractCollection::isEmpty);
  }

  @Test
  public void testMergeFeature() {
    localDataStore.insertOrUpdateUser(FAKE_USER).subscribe();

    Project project = FAKE_PROJECT;
    localDataStore.insertOrUpdateProject(project).subscribe();

    FeatureMutation mutation = FAKE_FEATURE_MUTATION;
    localDataStore.applyAndEnqueue(mutation).subscribe();

    Feature feature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();
    Point point = Point.newBuilder().setLongitude(11.0).setLatitude(33.0).build();
    feature = feature.toBuilder().setPoint(point).build();
    localDataStore.mergeFeature(feature).test().assertComplete();

    Feature newFeature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(mutation.getFeatureId(), newFeature.getId());
    Assert.assertEquals(project, newFeature.getProject());
    Assert.assertEquals(FAKE_LAYER.getItemLabel(), newFeature.getTitle());
    Assert.assertEquals(FAKE_LAYER, newFeature.getLayer());
    Assert.assertNull(newFeature.getCustomId());
    Assert.assertNull(newFeature.getCaption());
    Assert.assertEquals(point, newFeature.getPoint());
    Assert.assertEquals(FAKE_USER, newFeature.getCreated().getUser());
    Assert.assertEquals(FAKE_USER, newFeature.getLastModified().getUser());
  }

  @Test
  public void testApplyAndEnqueue_observationMutation() {
    localDataStore.insertOrUpdateUser(FAKE_USER).subscribe();
    localDataStore.insertOrUpdateProject(FAKE_PROJECT).subscribe();
    localDataStore.applyAndEnqueue(FAKE_FEATURE_MUTATION).subscribe();

    ObservationMutation mutation = FAKE_OBSERVATION_MUTATION;
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    ImmutableList<Mutation> savedMutations =
        localDataStore.getPendingMutations(mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(2, savedMutations.size());
    // ignoring the first item, which is a FeatureMutation. Already tested separately.
    assertObservationMutation(mutation, (ObservationMutation) savedMutations.get(1));

    // check if the observation was saved properly to local database
    Feature feature =
        localDataStore.getFeature(FAKE_PROJECT, mutation.getFeatureId()).blockingGet();
    Observation observation =
        localDataStore.getObservation(feature, mutation.getObservationId()).blockingGet();
    Assert.assertEquals(mutation.getObservationId(), observation.getId());
    Assert.assertEquals(FAKE_USER, observation.getCreated().getUser());
    Assert.assertEquals(feature, observation.getFeature());
    Assert.assertEquals(FAKE_FORM, observation.getForm());
    Assert.assertEquals(FAKE_PROJECT, observation.getProject());
    Assert.assertEquals(FAKE_USER, observation.getLastModified().getUser());
    MatcherAssert.assertThat(
        ResponseMap.builder().applyDeltas(mutation.getResponseDeltas()).build(),
        samePropertyValuesAs(observation.getResponses()));

    // now update the inserted observation with new responses
    ImmutableList<ResponseDelta> deltas =
        ImmutableList.of(
            ResponseDelta.builder()
                .setFieldId("really new field")
                .setNewResponse(TextResponse.fromString("value for the really new field"))
                .build());
    mutation = mutation.toBuilder().setResponseDeltas(deltas).setType(Mutation.Type.UPDATE).build();
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    savedMutations = localDataStore.getPendingMutations(mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(3, savedMutations.size());

    // ignoring the first item, which is a FeatureMutation. Already tested separately.
    assertObservationMutation(mutation, (ObservationMutation) savedMutations.get(2));

    // check if the observation was updated in the local database
    observation = localDataStore.getObservation(feature, mutation.getObservationId()).blockingGet();
    Assert.assertEquals(mutation.getObservationId(), observation.getId());
    Assert.assertEquals(FAKE_USER, observation.getCreated().getUser());
    Assert.assertEquals(feature, observation.getFeature());
    Assert.assertEquals(FAKE_FORM, observation.getForm());
    Assert.assertEquals(FAKE_PROJECT, observation.getProject());
    Assert.assertEquals(FAKE_USER, observation.getLastModified().getUser());
    MatcherAssert.assertThat(
        ResponseMap.builder().applyDeltas(deltas).build(),
        samePropertyValuesAs(observation.getResponses()));

    // also test that getObservations returns the same observation as well
    ImmutableList<Observation> observations =
        localDataStore.getObservations(feature, FAKE_FORM.getId()).blockingGet();
    Assert.assertEquals(1, observations.size());
    Assert.assertEquals(observation.getId(), observations.get(0).getId());
  }

  @Test
  public void testMergeObservation() {
    localDataStore.insertOrUpdateUser(FAKE_USER).subscribe();
    localDataStore.insertOrUpdateProject(FAKE_PROJECT).subscribe();
    localDataStore.applyAndEnqueue(FAKE_FEATURE_MUTATION).subscribe();

    ObservationMutation mutation = FAKE_OBSERVATION_MUTATION;
    localDataStore.applyAndEnqueue(mutation).subscribe();

    Feature feature =
        localDataStore.getFeature(FAKE_PROJECT, mutation.getFeatureId()).blockingGet();

    ResponseMap responseMap =
        ResponseMap.builder()
            .putResponse("foo field", TextResponse.fromString("foo value").get())
            .build();

    Observation observation =
        localDataStore.getObservation(feature, mutation.getObservationId()).blockingGet();
    observation = observation.toBuilder().setResponses(responseMap).build();

    localDataStore.mergeObservation(observation).test().assertComplete();

    ResponseMap result =
        localDataStore
            .getObservation(feature, observation.getId())
            .test()
            .values()
            .get(0)
            .getResponses();
    Assert.assertEquals("foo value", result.getResponse("foo field").get().toString());
  }

  @Test
  public void testGetTile() {
    Tile tile =
        Tile.newBuilder()
            .setId("tile id_1")
            .setPath("/foo/path1")
            .setState(State.PENDING)
            .setUrl("foo_url")
            .build();
    localDataStore.insertOrUpdateTile(tile).subscribe();
    localDataStore.getTile("tile id_1").test().assertValueCount(1).assertValue(tile);
  }

  @Test
  public void testGetTilesOnceAndStream() {
    TestSubscriber<ImmutableSet<Tile>> subscriber = localDataStore.getTilesOnceAndStream().test();

    subscriber.assertValueCount(1);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);

    Tile tile1 =
        Tile.newBuilder()
            .setId("tile_id_1")
            .setPath("/foo/path1")
            .setState(State.PENDING)
            .setUrl("foo_url")
            .build();
    Tile tile2 =
        Tile.newBuilder()
            .setId("tile_id_2")
            .setPath("/foo/path2")
            .setState(State.PENDING)
            .setUrl("foo_url")
            .build();
    localDataStore.insertOrUpdateTile(tile1).subscribe();
    localDataStore.insertOrUpdateTile(tile2).subscribe();

    subscriber.assertValueCount(3);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);
    subscriber.assertValueAt(1, ImmutableSet.of(tile1));
    subscriber.assertValueAt(2, ImmutableSet.of(tile1, tile2));
  }

  @Test
  public void testGetPendingTile() {
    Tile tile1 =
        Tile.newBuilder()
            .setId("id_1")
            .setState(State.PENDING)
            .setPath("some_path")
            .setUrl("some_url")
            .build();
    Tile tile2 =
        Tile.newBuilder()
            .setId("id_2")
            .setState(State.DOWNLOADED)
            .setPath("some_path")
            .setUrl("some_url")
            .build();
    Tile tile3 =
        Tile.newBuilder()
            .setId("id_3")
            .setState(State.PENDING)
            .setPath("some_path")
            .setUrl("some_url")
            .build();
    localDataStore.insertOrUpdateTile(tile1).subscribe();
    localDataStore.insertOrUpdateTile(tile2).subscribe();
    localDataStore.insertOrUpdateTile(tile3).subscribe();
    localDataStore.getPendingTiles().test().assertValue(ImmutableList.of(tile1, tile3));
  }

  @Test
  public void testGetOfflineAreas() {
    LatLngBounds bounds1 = LatLngBounds.builder().include(new LatLng(0.0, 0.0)).build();
    OfflineArea area1 =
        OfflineArea.newBuilder()
            .setId("id_1")
            .setBounds(bounds1)
            .setState(OfflineArea.State.PENDING)
            .build();
    LatLngBounds bounds2 = LatLngBounds.builder().include(new LatLng(10.0, 30.0)).build();
    OfflineArea area2 =
        OfflineArea.newBuilder()
            .setId("id_2")
            .setBounds(bounds2)
            .setState(OfflineArea.State.PENDING)
            .build();
    localDataStore.insertOrUpdateOfflineArea(area1).subscribe();
    localDataStore.insertOrUpdateOfflineArea(area2).subscribe();
    localDataStore.getOfflineAreas().test().assertValue(ImmutableList.of(area1, area2));
  }
}
