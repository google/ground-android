package com.google.android.gnd.persistence.remote;

import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;

import com.google.android.gnd.model.form.Field.Type;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.rx.ValueOrError;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class FakeRemoteDataStore implements RemoteDataStore {

  private final Element textElement = Element.ofField(Field.newBuilder()
    .setId("TEST_FIELD")
    .setLabel("Test field")
    .setRequired(true)
    .setType(Type.TEXT)
    .build());

  private final Layer layerWithTextField = Layer.newBuilder()
    .setId("LAYER_TEXT_FIELD")
    .setName("Layer text field")
    .setForm(Form.newBuilder()
      .setElements(ImmutableList.of(textElement))
      .build())
    .setDefaultStyle(Style.builder()
      .setColor("#0000ff")
      .build())
    .build();

  private final Layer layerWithNoFields = Layer.newBuilder()
    .setId("LAYER_NO_FIELDS")
    .setName("Layer no fields")
    .setDefaultStyle(Style.builder()
      .setColor("#00ff00")
      .build())
    .build();

  private final Project testProject = Project.newBuilder()
    .setId("TEST_ID")
    .setTitle("TEST TITLE")
    .setDescription("TEST DESCRIPTION")
    .putLayer("LAYER_NO_FIELDS", layerWithNoFields)
    .build();

  @Inject
  FakeRemoteDataStore() {
  }

  @Override
  public Single<List<Project>> loadProjectSummaries(
    User user) {
    return Single.just(Collections.singletonList(testProject));
  }

  @Override
  public Single<Project> loadProject(String projectId) {
    return Single.just(testProject);
  }

  @Override
  public Flowable<RemoteDataEvent<Feature>> loadFeaturesOnceAndStreamChanges(
    Project project) {

    /*Point point = Point.newBuilder()
      .setLatitude(51.510357)
      .setLongitude(-0.116773)
      .build();

    Feature feature = Feature.newBuilder()
      .setId("FEATURE_ID")
      .setProject(project)
      .setPoint(point)
      .setLastModified(AuditInfo.now(FakeAuthenticationManager.TEST_USER))
      // Need to add layer info
      .build();*/
//    return Flowable.just(RemoteDataEvent.loaded("ENTITY_ID", feature));
    return Flowable.empty();
  }

  @Override
  public Single<ImmutableList<ValueOrError<Observation>>> loadObservations(
    Feature feature) {
    return null;
  }

  @Override
  public Completable applyMutations(
    ImmutableCollection<Mutation> mutations,
    User user) {
    return null;
  }
}
