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

package com.google.android.gnd.persistence.remote;

import com.google.android.gnd.FakeData;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.TermsOfService;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.rx.ValueOrError;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class FakeRemoteDataStore implements RemoteDataStore {

  private final Layer layerWithNoForm =
      Layer.newBuilder()
          .setId(FakeData.LAYER_NO_FORM_ID)
          .setName(FakeData.LAYER_NO_FORM_NAME)
          .setDefaultStyle(Style.builder().setColor(FakeData.LAYER_NO_FORM_COLOR).build())
          .setContributorsCanAdd(ImmutableList.of(FeatureType.POINT))
          .build();

  private final Project testProjectWithLayerAndNoForm =
      Project.newBuilder()
          .setId(FakeData.PROJECT_ID_WITH_LAYER_AND_NO_FORM)
          .setTitle(FakeData.PROJECT_TITLE)
          .setDescription(FakeData.PROJECT_DESCRIPTION)
          .putLayer(FakeData.LAYER_NO_FORM_ID, layerWithNoForm)
          .setAcl(ImmutableMap.of(FakeData.TEST_USER.getEmail(), "contributor"))
          .build();

  private final TermsOfService testTermsOfService =
      TermsOfService.builder()
          .setId(FakeData.TERMS_OF_SERVICE_ID)
          .setText(FakeData.TERMS_OF_SERVICE)
          .build();

  private final Project testProjectWithNoLayers =
      Project.newBuilder()
          .setId(FakeData.PROJECT_ID_WITH_NO_LAYERS)
          .setTitle(FakeData.PROJECT_TITLE)
          .setDescription(FakeData.PROJECT_DESCRIPTION)
          .setAcl(ImmutableMap.of(FakeData.TEST_USER.getEmail(), "contributor"))
          .build();

  private String activeProjectId = FakeData.PROJECT_ID_WITH_LAYER_AND_NO_FORM;

  @Inject
  FakeRemoteDataStore() {}

  /**
   * Set this before the test scenario is loaded.
   *
   * <p>In that case, launch scenario manually using ActivityScenario.launch instead of using
   * ActivityScenarioRule.
   */
  public void setActiveProjectId(String projectId) {
    activeProjectId = projectId;
  }

  private Project getTestProject() {
    switch (activeProjectId) {
      case FakeData.PROJECT_ID_WITH_LAYER_AND_NO_FORM:
        return testProjectWithLayerAndNoForm;
      case FakeData.PROJECT_ID_WITH_NO_LAYERS:
        return testProjectWithNoLayers;
      default:
        return null;
    }
  }

  @Override
  public Single<List<Project>> loadProjectSummaries(User user) {
    return Single.just(Collections.singletonList(getTestProject()));
  }

  @Override
  public Single<Project> loadProject(String projectId) {
    return Single.just(getTestProject());
  }

  @Override
  public @Cold Maybe<TermsOfService> loadTermsOfService() {
    return Maybe.just(testTermsOfService);
  }

  @Override
  public Flowable<RemoteDataEvent<Feature>> loadFeaturesOnceAndStreamChanges(Project project) {
    return Flowable.empty();
  }

  @Override
  public Single<ImmutableList<ValueOrError<Observation>>> loadObservations(Feature feature) {
    return null;
  }

  @Override
  public Completable applyMutations(ImmutableCollection<Mutation> mutations, User user) {
    return null;
  }
}
