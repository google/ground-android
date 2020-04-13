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
import com.google.android.gnd.model.form.MultipleChoice.Builder;
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

  // This rule makes sure that Room executes all the database operations instantly.
  @Rule public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  @Inject LocalDataStore localDataStore;

  @Before
  public void setUp() {
    DaggerTestComponent.create().inject(this);
  }

  private User createUser() {
    return User.builder()
        .setId("test_user_id")
        .setEmail("test@gmail.com")
        .setDisplayName("test user")
        .build();
  }

  private Project createProject() {
    Builder multipleChoiceBuilder =
        MultipleChoice.newBuilder().setCardinality(Cardinality.SELECT_ONE);
    multipleChoiceBuilder
        .optionsBuilder()
        .add(Option.newBuilder().setCode("a").setLabel("Name").build())
        .add(Option.newBuilder().setCode("b").setLabel("Age").build());

    Field field =
        Field.newBuilder()
            .setId("field id")
            .setLabel("field label")
            .setRequired(false)
            .setType(Type.MULTIPLE_CHOICE)
            .setMultipleChoice(multipleChoiceBuilder.build())
            .build();

    Element element = Element.ofField(field);

    Form form =
        Form.newBuilder()
            .setId("form id")
            .setElements(ImmutableList.<Element>builder().add(element).build())
            .build();

    Layer layer =
        Layer.newBuilder()
            .setId("layer id")
            .setItemLabel("item label")
            .setListHeading("heading title")
            .setDefaultStyle(Style.builder().setColor("000").build())
            .setForm(form)
            .build();

    Project.Builder builder =
        Project.newBuilder()
            .setId("project id")
            .setTitle("project 1")
            .setDescription("foo description");
    builder.putLayer(layer.getId(), layer);
    return builder.build();
  }

  private FeatureMutation createFeatureMutation(String userId, String projectId, String layerId) {
    return FeatureMutation.builder()
        .setType(Mutation.Type.CREATE)
        .setUserId(userId)
        .setProjectId(projectId)
        .setFeatureId("feature id")
        .setLayerId(layerId)
        .setNewLocation(
            Optional.ofNullable(Point.newBuilder().setLatitude(110.0).setLongitude(-23.1).build()))
        .setClientTimestamp(new Date())
        .build();
  }

  private ObservationMutation createObservationMutation(
      String projectId, String featureId, String layerId, String formId, String userId) {
    return ObservationMutation.builder()
        .setType(Mutation.Type.CREATE)
        .setProjectId(projectId)
        .setFeatureId(featureId)
        .setLayerId(layerId)
        .setObservationId("observation id")
        .setFormId(formId)
        .setResponseDeltas(
            ImmutableList.<ResponseDelta>builder()
                .add(
                    ResponseDelta.builder()
                        .setFieldId("field id")
                        .setNewResponse(TextResponse.fromString("response for field id"))
                        .build())
                .build())
        .setClientTimestamp(new Date())
        .setUserId(userId)
        .build();
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
    localDataStore.insertOrUpdateProject(project1).test().assertComplete();
    localDataStore.insertOrUpdateProject(project2).test().assertComplete();
    localDataStore
        .getProjects()
        .test()
        .assertValue(ImmutableList.<Project>builder().add(project1, project2).build());
  }

  @Test
  public void testGetProjectById() {
    Project project = createProject();
    localDataStore.insertOrUpdateProject(project).test().assertComplete();
    localDataStore.getProjectById(project.getId()).test().assertValue(project);
  }

  @Test
  public void testDeleteProject() {
    Project project = createProject();
    localDataStore.insertOrUpdateProject(project).test().assertComplete();
    localDataStore.deleteProject(project).test().assertComplete();
    localDataStore.getProjects().test().assertValue(AbstractCollection::isEmpty);
  }

  @Test
  public void testGetUser() {
    User user = createUser();
    localDataStore.insertOrUpdateUser(user).test().assertComplete();
    localDataStore.getUser(user.getId()).test().assertValue(user);
  }

  @Test
  public void testApplyAndEnqueue_featureMutation() {
    User user = createUser();
    localDataStore.insertOrUpdateUser(user).test().assertComplete();

    Project project = createProject();
    localDataStore.insertOrUpdateProject(project).test().assertComplete();

    Layer layer = project.getLayers().get(0);
    FeatureMutation mutation = createFeatureMutation(user.getId(), project.getId(), layer.getId());
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    ImmutableList<Mutation> savedMutations =
        localDataStore.getPendingMutations(mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(1, savedMutations.size());

    // assert that mutation is saved to local database
    FeatureMutation savedMutation = (FeatureMutation) savedMutations.get(0);
    Assert.assertEquals(mutation.getNewLocation(), savedMutation.getNewLocation());
    Assert.assertEquals(mutation.getType(), savedMutation.getType());
    Assert.assertEquals(mutation.getUserId(), savedMutation.getUserId());
    Assert.assertEquals(mutation.getProjectId(), savedMutation.getProjectId());
    Assert.assertEquals(mutation.getFeatureId(), savedMutation.getFeatureId());
    Assert.assertEquals(mutation.getLayerId(), savedMutation.getLayerId());
    Assert.assertEquals(mutation.getClientTimestamp(), savedMutation.getClientTimestamp());
    Assert.assertEquals(0, savedMutation.getRetryCount());
    Assert.assertNull(savedMutation.getLastError());

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
    User user = createUser();
    localDataStore.insertOrUpdateUser(user).test().assertComplete();

    Project project = createProject();
    localDataStore.insertOrUpdateProject(project).test().assertComplete();

    TestSubscriber<ImmutableSet<Feature>> subscriber =
        localDataStore.getFeaturesOnceAndStream(project).test();

    subscriber.assertValueCount(1);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);

    Layer layer = project.getLayers().get(0);
    FeatureMutation mutation = createFeatureMutation(user.getId(), project.getId(), layer.getId());
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    Feature feature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();

    subscriber.assertValueCount(2);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);
    subscriber.assertValueAt(1, ImmutableSet.<Feature>builder().add(feature).build());
  }

  @Test
  public void testUpdateMutations() {
    User user = createUser();
    localDataStore.insertOrUpdateUser(user).test().assertComplete();

    Project project = createProject();
    localDataStore.insertOrUpdateProject(project).test().assertComplete();

    Layer layer = project.getLayers().get(0);
    FeatureMutation mutation = createFeatureMutation(user.getId(), project.getId(), layer.getId());
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    Mutation insertedMutation =
        localDataStore.getPendingMutations(mutation.getFeatureId()).blockingGet().get(0);
    Point newPoint = Point.newBuilder().setLatitude(51.0).setLongitude(44.0).build();
    Mutation updatedMutation =
        ((FeatureMutation) insertedMutation)
            .toBuilder()
            .setNewLocation(Optional.ofNullable(newPoint))
            .build();

    localDataStore
        .updateMutations(ImmutableList.<Mutation>builder().add(updatedMutation).build())
        .test()
        .assertComplete();

    ImmutableList<Mutation> savedMutations =
        localDataStore.getPendingMutations(updatedMutation.getFeatureId()).blockingGet();
    Assert.assertEquals(1, savedMutations.size());

    FeatureMutation savedMutation = (FeatureMutation) savedMutations.get(0);
    Assert.assertEquals(newPoint, savedMutation.getNewLocation().get());
  }

  @Test
  public void testRemovePendingMutation() {
    User user = createUser();
    localDataStore.insertOrUpdateUser(user).test().assertComplete();

    Project project = createProject();
    localDataStore.insertOrUpdateProject(project).test().assertComplete();

    Layer layer = project.getLayers().get(0);
    FeatureMutation mutation = createFeatureMutation(user.getId(), project.getId(), layer.getId());
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    localDataStore
        .removePendingMutations(
            localDataStore.getPendingMutations(mutation.getFeatureId()).blockingGet())
        .test()
        .assertComplete();

    localDataStore
        .getPendingMutations(mutation.getFeatureId())
        .test()
        .assertValue(AbstractCollection::isEmpty);
  }

  @Test
  public void testMergeFeature() {
    User user = createUser();
    localDataStore.insertOrUpdateUser(user).test().assertComplete();

    Project project = createProject();
    localDataStore.insertOrUpdateProject(project).test().assertComplete();

    Layer layer = project.getLayers().get(0);
    FeatureMutation mutation = createFeatureMutation(user.getId(), project.getId(), layer.getId());
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    Feature feature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();
    Point point = Point.newBuilder().setLongitude(11.0).setLatitude(33.0).build();
    feature = feature.toBuilder().setPoint(point).build();
    localDataStore.mergeFeature(feature).test().assertComplete();

    Feature newFeature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(mutation.getFeatureId(), newFeature.getId());
    Assert.assertEquals(project, newFeature.getProject());
    Assert.assertEquals(layer.getItemLabel(), newFeature.getTitle());
    Assert.assertEquals(layer, newFeature.getLayer());
    Assert.assertNull(newFeature.getCustomId());
    Assert.assertNull(newFeature.getCaption());
    Assert.assertEquals(point, newFeature.getPoint());
    Assert.assertEquals(user, newFeature.getCreated().getUser());
    Assert.assertEquals(user, newFeature.getLastModified().getUser());
  }

  @Test
  public void testApplyAndEnqueue_observationMutation() {
    User user = createUser();
    localDataStore.insertOrUpdateUser(user).test().assertComplete();

    Project project = createProject();
    localDataStore.insertOrUpdateProject(project).test().assertComplete();

    Layer layer = project.getLayers().get(0);
    FeatureMutation featureMutation =
        createFeatureMutation(user.getId(), project.getId(), layer.getId());
    localDataStore.applyAndEnqueue(featureMutation).test().assertComplete();

    ObservationMutation mutation =
        createObservationMutation(
            project.getId(),
            featureMutation.getFeatureId(),
            layer.getId(),
            layer.getForm().get().getId(),
            user.getId());
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    ImmutableList<Mutation> savedMutations =
        localDataStore.getPendingMutations(mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(2, savedMutations.size());

    // ignoring the first item, which is a FeatureMutation. Already tested separately.
    ObservationMutation savedMutation = (ObservationMutation) savedMutations.get(1);
    Assert.assertEquals(mutation.getResponseDeltas(), savedMutation.getResponseDeltas());
    Assert.assertEquals(mutation.getType(), savedMutation.getType());
    Assert.assertEquals(mutation.getUserId(), savedMutation.getUserId());
    Assert.assertEquals(mutation.getProjectId(), savedMutation.getProjectId());
    Assert.assertEquals(mutation.getFeatureId(), savedMutation.getFeatureId());
    Assert.assertEquals(layer.getId(), savedMutation.getLayerId());
    Assert.assertEquals(mutation.getClientTimestamp(), savedMutation.getClientTimestamp());
    Assert.assertEquals(0, savedMutation.getRetryCount());
    Assert.assertNull(savedMutation.getLastError());

    // check if the observation was saved properly to local database
    Feature feature = localDataStore.getFeature(project, mutation.getFeatureId()).blockingGet();
    Observation observation =
        localDataStore.getObservation(feature, mutation.getObservationId()).blockingGet();
    Assert.assertEquals(mutation.getObservationId(), observation.getId());
    Assert.assertEquals(user, observation.getCreated().getUser());
    Assert.assertEquals(feature, observation.getFeature());
    Assert.assertEquals(layer.getForm().get(), observation.getForm());
    Assert.assertEquals(project, observation.getProject());
    Assert.assertEquals(user, observation.getLastModified().getUser());
    MatcherAssert.assertThat(
        ResponseMap.builder().applyDeltas(mutation.getResponseDeltas()).build(),
        samePropertyValuesAs(observation.getResponses()));

    // now update the inserted observation with new responses
    ImmutableList<ResponseDelta> deltas =
        ImmutableList.<ResponseDelta>builder()
            .add(
                ResponseDelta.builder()
                    .setFieldId("really new field")
                    .setNewResponse(TextResponse.fromString("value for the really new field"))
                    .build())
            .build();
    mutation = mutation.toBuilder().setResponseDeltas(deltas).setType(Mutation.Type.UPDATE).build();
    localDataStore.applyAndEnqueue(mutation).test().assertComplete();

    savedMutations = localDataStore.getPendingMutations(mutation.getFeatureId()).blockingGet();
    Assert.assertEquals(3, savedMutations.size());

    // ignoring the first item, which is a FeatureMutation. Already tested separately.
    savedMutation = (ObservationMutation) savedMutations.get(2);
    Assert.assertEquals(deltas, savedMutation.getResponseDeltas());
    Assert.assertEquals(mutation.getType(), savedMutation.getType());
    Assert.assertEquals(mutation.getUserId(), savedMutation.getUserId());
    Assert.assertEquals(mutation.getProjectId(), savedMutation.getProjectId());
    Assert.assertEquals(mutation.getFeatureId(), savedMutation.getFeatureId());
    Assert.assertEquals(mutation.getLayerId(), savedMutation.getLayerId());
    Assert.assertEquals(mutation.getClientTimestamp(), savedMutation.getClientTimestamp());
    Assert.assertEquals(0, savedMutation.getRetryCount());
    Assert.assertNull(savedMutation.getLastError());

    // check if the observation was updated in the local database
    observation = localDataStore.getObservation(feature, mutation.getObservationId()).blockingGet();
    Assert.assertEquals(mutation.getObservationId(), observation.getId());
    Assert.assertEquals(user, observation.getCreated().getUser());
    Assert.assertEquals(feature, observation.getFeature());
    Assert.assertEquals(layer.getForm().get(), observation.getForm());
    Assert.assertEquals(project, observation.getProject());
    Assert.assertEquals(user, observation.getLastModified().getUser());
    MatcherAssert.assertThat(
        ResponseMap.builder().applyDeltas(deltas).build(),
        samePropertyValuesAs(observation.getResponses()));

    // also test that getObservations returns the same observation as well
    ImmutableList<Observation> observations =
        localDataStore.getObservations(feature, layer.getForm().get().getId()).blockingGet();
    Assert.assertEquals(1, observations.size());
    Assert.assertEquals(observation.getId(), observations.get(0).getId());
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
    localDataStore.insertOrUpdateTile(tile).test().assertComplete();
    localDataStore.getTile("tile id_1").test().assertValueCount(1).assertValue(tile);

    tile = tile.toBuilder().setPath("/foo/path2").build();
    localDataStore.insertOrUpdateTile(tile).test().assertComplete();
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
    localDataStore.insertOrUpdateTile(tile1).test().assertComplete();
    localDataStore.insertOrUpdateTile(tile2).test().assertComplete();

    subscriber.assertValueCount(3);
    subscriber.assertValueAt(0, AbstractCollection::isEmpty);
    subscriber.assertValueAt(1, ImmutableSet.<Tile>builder().add(tile1).build());
    subscriber.assertValueAt(2, ImmutableSet.<Tile>builder().add(tile1, tile2).build());
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
    localDataStore.insertOrUpdateTile(tile1).test().assertComplete();
    localDataStore.insertOrUpdateTile(tile2).test().assertComplete();
    localDataStore.insertOrUpdateTile(tile3).test().assertComplete();

    localDataStore
        .getPendingTiles()
        .test()
        .assertValue(ImmutableList.<Tile>builder().add(tile1, tile3).build());
  }

  @Test
  public void testInsertOrUpdateOfflineAreas() {
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

    localDataStore.insertOrUpdateOfflineArea(area1).test().assertComplete();
    localDataStore.insertOrUpdateOfflineArea(area2).test().assertComplete();
    localDataStore
        .getOfflineAreas()
        .test()
        .assertValue(ImmutableList.<OfflineArea>builder().add(area1, area2).build());
  }
}
