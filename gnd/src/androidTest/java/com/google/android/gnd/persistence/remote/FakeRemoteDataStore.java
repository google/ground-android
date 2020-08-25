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
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
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

  private final Layer layerWithNoFields = Layer.newBuilder()
    .setId(FakeData.LAYER_NO_FIELDS_ID)
    .setName(FakeData.LAYER_NO_FIELDS_NAME)
    .setDefaultStyle(Style.builder()
      .setColor(FakeData.LAYER_NO_FIELDS_COLOR)
      .build())
    .build();

  private final Project testProject = Project.newBuilder()
    .setId(FakeData.PROJECT_ID)
    .setTitle(FakeData.PROJECT_TITLE)
    .setDescription(FakeData.PROJECT_DESCRIPTION)
    .putLayer(FakeData.LAYER_NO_FIELDS_ID, layerWithNoFields)
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
