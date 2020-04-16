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

  private static final User TEST_USER =
      User.builder().setId("user id").setEmail("user@gmail.com").setDisplayName("user 1").build();

  private static final MultipleChoice TEST_MULTIPLE_CHOICE =
      MultipleChoice.newBuilder()
          .setCardinality(Cardinality.SELECT_ONE)
          .setOptions(
              ImmutableList.of(
                  Option.newBuilder().setCode("a").setLabel("Name").build(),
                  Option.newBuilder().setCode("b").setLabel("Age").build()))
          .build();

  private static final Field TEST_FIELD =
      Field.newBuilder()
          .setId("field id")
          .setLabel("field label")
          .setRequired(false)
          .setType(Type.MULTIPLE_CHOICE)
          .setMultipleChoice(TEST_MULTIPLE_CHOICE)
          .build();

  private static final Form TEST_FORM =
      Form.newBuilder()
          .setId("form id")
          .setElements(ImmutableList.of(Element.ofField(TEST_FIELD)))
          .build();

  private static final Layer TEST_LAYER =
      Layer.newBuilder()
          .setId("layer id")
          .setItemLabel("item label")
          .setListHeading("heading title")
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

  private static final FeatureMutation TEST_FEATURE_MUTATION =
      FeatureMutation.builder()
          .setId(1L)
          .setFeatureId("feature id")
          .setType(Mutation.Type.CREATE)
          .setUserId("user id")
          .setProjectId("project id")
          .setLayerId("layer id")
          .setNewLocation(Optional.ofNullable(TEST_POINT))
          .setClientTimestamp(new Date())
          .build();

  private static final ObservationMutation TEST_OBSERVATION_MUTATION =
      ObservationMutation.builder()
          .setObservationId("observation id")
          .setType(Mutation.Type.CREATE)
          .setProjectId("project id")
          .setFeatureId("feature id")
          .setLayerId("layer id")
          .setFormId("form id")
          .setUserId("user id")
          .setResponseDeltas(
              ImmutableList.of(
                  ResponseDelta.builder()
                      .setFieldId("field id")
                      .setNewResponse(TextResponse.fromString("response for field id"))
                      .build()))
          .setClientTimestamp(new Date())
          .build();

  private static final Tile TEST_TILE_PENDING =
      Tile.newBuilder()
          .setId("id_1")
          .setState(State.PENDING)
          .setPath("some_path 1")
          .setUrl("some_url 1")
          .build();

  private static final Tile TEST_TILE_DOWNLOADED =
      Tile.newBuilder()
          .setId("id_2")
          .setState(State.DOWNLOADED)
          .setPath("some_path 2")
          .setUrl("some_url 2")
          .build();

  private static final Tile TEST_TILE_FAILED =
      Tile.newBuilder()
          .setId("id_3")
          .setState(State.FAILED)
          .setPath("some_path 3")
          .setUrl("some_url 3")
          .build();

  private static final OfflineArea TEST_OFFLINE_AREA =
      OfflineArea.newBuilder()
          .setId("id_1")
          .setBounds(LatLngBounds.builder().include(new LatLng(0.0, 0.0)).build())
          .setState(OfflineArea.State.PENDING)
          .build();

  // This rule makes sure that Room executes all the database operations instantly.
  @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @Inject LocalDataStore localDataStore;

  private static void assertObservationMutation(
      ObservationMutation expected, ObservationMutation actual) {
    // TODO: Id is auto-assigned to ObservationMutation.
    //  If we try to give it while inserting, then it causes problems. Improve this behavior.
    //  So, copy the id from actual to expected and then compare the objects.
    expected = expected.toBuilder().setId(actual.getId()).build();
    Assert.assertEquals(expected, actual);
  }

  private static void assertObservation(ObservationMutation mutation, Observation observation) {
    Assert.assertEquals(mutation.getObservationId(), observation.getId());
    Assert.assertEquals(mutation.getFeatureId(), observation.getFeature().getId());
    Assert.assertEquals(TEST_USER, observation.getCreated().getUser());
    Assert.assertEquals(TEST_FORM, observation.getForm());
    Assert.assertEquals(TEST_PROJECT, observation.getProject());
    Assert.assertEquals(TEST_USER, observation.getLastModified().getUser());
    MatcherAssert.assertThat(
        ResponseMap.builder().applyDeltas(mutation.getResponseDeltas()).build(),
        samePropertyValuesAs(observation.getResponses()));
  }

  @Before
  public void setUp() {
    DaggerTestComponent.create().inject(this);
  }

  @Test
  public void testGetProjects() {
    Project project = TEST_PROJECT;
    localDataStore.insertOrUpdateProject(project).subscribe();
    localDataStore.getProjects().test().assertValue(ImmutableList.of(project));
  }

  @Test
  public void testGetProjectById() {
    Project project = TEST_PROJECT;
    localDataStore.insertOrUpdateProject(project).subscribe();
    localDataStore.getProjectById(project.getId()).test().assertValue(project);
  }

  @Test
  public void testDeleteProject() {
    Project project = TEST_PROJECT;
    localDataStore.insertOrUpdateProject(project).subscribe();
    localDataStore.deleteProject(project).test().assertComplete();
    localDataStore.getProjects().test().assertValue(AbstractCollection::isEmpty);
  }

  @Test
  public void testGetUser() {
    User user = TEST_USER;
    localDataStore.insertOrUpdateUser(user).subscribe();
    localDataStore.getUser(user.getId()).test().assertValue(user);
  }

  @Test
  public void testApplyAndEnqueue_featureMutation() {
    localDataStore.insertOrUpdateUser(TEST_USER).subscribe();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).subscribe();

    FeatureMutation mutation = TEST_FEATURE_MUTATION;
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    // assert that mutation is saved to local database
    localDataStore
        .getPendingMutations(mutation.getFeatureId())
        .test()
        .assertValue(ImmutableList.of(mutation));

    // assert feature is saved to local database
    Feature feature =
        localDataStore.getFeature(TEST_PROJECT, mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(mutation.getFeatureId(), feature.getId());
    Assert.assertEquals(TEST_PROJECT, feature.getProject());
    Assert.assertEquals(TEST_LAYER.getItemLabel(), feature.getTitle());
    Assert.assertEquals(TEST_LAYER, feature.getLayer());
    Assert.assertNull(feature.getCustomId());
    Assert.assertNull(feature.getCaption());
    Assert.assertEquals(TEST_POINT, feature.getPoint());
    Assert.assertEquals(TEST_USER, feature.getCreated().getUser());
    Assert.assertEquals(TEST_USER, feature.getLastModified().getUser());
  }

  @Test
  public void testGetFeaturesOnceAndStream() {
    localDataStore.insertOrUpdateUser(TEST_USER).subscribe();

    Project project = TEST_PROJECT;
    localDataStore.insertOrUpdateProject(project).subscribe();

    TestSubscriber<ImmutableSet<Feature>> subscriber =
        localDataStore.getFeaturesOnceAndStream(project).test();

    subscriber.assertValueCount(1);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);

    FeatureMutation mutation = TEST_FEATURE_MUTATION;
    localDataStore.applyAndEnqueue(mutation).subscribe();

    Feature feature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();

    subscriber.assertValueCount(2);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);
    subscriber.assertValueAt(1, ImmutableSet.of(feature));
  }

  @Test
  public void testUpdateMutations() {
    localDataStore.insertOrUpdateUser(TEST_USER).subscribe();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).subscribe();

    FeatureMutation mutation = TEST_FEATURE_MUTATION;
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
    localDataStore.insertOrUpdateUser(TEST_USER).subscribe();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).subscribe();

    FeatureMutation mutation = TEST_FEATURE_MUTATION;
    localDataStore.applyAndEnqueue(mutation).subscribe();

    localDataStore.removePendingMutations(ImmutableList.of(mutation)).test().assertComplete();

    localDataStore
        .getPendingMutations(mutation.getFeatureId())
        .test()
        .assertValue(AbstractCollection::isEmpty);
  }

  @Test
  public void testMergeFeature() {
    localDataStore.insertOrUpdateUser(TEST_USER).subscribe();

    Project project = TEST_PROJECT;
    localDataStore.insertOrUpdateProject(project).subscribe();

    FeatureMutation mutation = TEST_FEATURE_MUTATION;
    localDataStore.applyAndEnqueue(mutation).subscribe();

    Feature feature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();
    Point point = Point.newBuilder().setLongitude(11.0).setLatitude(33.0).build();
    feature = feature.toBuilder().setPoint(point).build();
    localDataStore.mergeFeature(feature).test().assertComplete();

    Feature newFeature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(mutation.getFeatureId(), newFeature.getId());
    Assert.assertEquals(project, newFeature.getProject());
    Assert.assertEquals(TEST_LAYER.getItemLabel(), newFeature.getTitle());
    Assert.assertEquals(TEST_LAYER, newFeature.getLayer());
    Assert.assertNull(newFeature.getCustomId());
    Assert.assertNull(newFeature.getCaption());
    Assert.assertEquals(point, newFeature.getPoint());
    Assert.assertEquals(TEST_USER, newFeature.getCreated().getUser());
    Assert.assertEquals(TEST_USER, newFeature.getLastModified().getUser());
  }

  @Test
  public void testApplyAndEnqueue_observationMutation() {
    localDataStore.insertOrUpdateUser(TEST_USER).subscribe();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).subscribe();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).subscribe();

    ObservationMutation mutation = TEST_OBSERVATION_MUTATION;
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    ImmutableList<Mutation> savedMutations =
        localDataStore.getPendingMutations(mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(2, savedMutations.size());
    // ignoring the first item, which is a FeatureMutation. Already tested separately.
    assertObservationMutation(mutation, (ObservationMutation) savedMutations.get(1));

    // check if the observation was saved properly to local database
    Feature feature =
        localDataStore.getFeature(TEST_PROJECT, mutation.getFeatureId()).blockingGet();
    Observation observation =
        localDataStore.getObservation(feature, mutation.getObservationId()).blockingGet();
    assertObservation(mutation, observation);

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
    assertObservation(mutation, observation);

    // also test that getObservations returns the same observation as well
    ImmutableList<Observation> observations =
        localDataStore.getObservations(feature, TEST_FORM.getId()).blockingGet();
    Assert.assertEquals(1, observations.size());
    assertObservation(mutation, observations.get(0));
  }

  @Test
  public void testMergeObservation() {
    localDataStore.insertOrUpdateUser(TEST_USER).subscribe();
    localDataStore.insertOrUpdateProject(TEST_PROJECT).subscribe();
    localDataStore.applyAndEnqueue(TEST_FEATURE_MUTATION).subscribe();

    ObservationMutation mutation = TEST_OBSERVATION_MUTATION;
    localDataStore.applyAndEnqueue(mutation).subscribe();

    Feature feature =
        localDataStore.getFeature(TEST_PROJECT, mutation.getFeatureId()).blockingGet();

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
    localDataStore.insertOrUpdateTile(TEST_TILE_PENDING).subscribe();
    localDataStore.getTile("id_1").test().assertValueCount(1).assertValue(TEST_TILE_PENDING);
  }

  @Test
  public void testGetTilesOnceAndStream() {
    TestSubscriber<ImmutableSet<Tile>> subscriber = localDataStore.getTilesOnceAndStream().test();

    subscriber.assertValueCount(1);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);

    localDataStore.insertOrUpdateTile(TEST_TILE_DOWNLOADED).subscribe();
    localDataStore.insertOrUpdateTile(TEST_TILE_PENDING).subscribe();

    subscriber.assertValueCount(3);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);
    subscriber.assertValueAt(1, ImmutableSet.of(TEST_TILE_DOWNLOADED));
    subscriber.assertValueAt(2, ImmutableSet.of(TEST_TILE_DOWNLOADED, TEST_TILE_PENDING));
  }

  @Test
  public void testGetPendingTile() {
    localDataStore.insertOrUpdateTile(TEST_TILE_DOWNLOADED).subscribe();
    localDataStore.insertOrUpdateTile(TEST_TILE_FAILED).subscribe();
    localDataStore.insertOrUpdateTile(TEST_TILE_PENDING).subscribe();
    localDataStore.getPendingTiles().test().assertValue(ImmutableList.of(TEST_TILE_PENDING));
  }

  @Test
  public void testGetOfflineAreas() {
    localDataStore.insertOrUpdateOfflineArea(TEST_OFFLINE_AREA).subscribe();
    localDataStore.getOfflineAreas().test().assertValue(ImmutableList.of(TEST_OFFLINE_AREA));
  }
}
