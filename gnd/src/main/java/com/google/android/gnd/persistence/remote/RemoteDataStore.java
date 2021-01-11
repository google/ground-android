/*
 * Copyright 2018 Google LLC
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

import androidx.annotation.Nullable;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.rx.ValueOrError;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.List;

/**
 * Defines API for accessing data in a remote data store. Implementations must ensure all
 * subscriptions are run in a background thread (i.e., not the Android main thread).
 */
public interface RemoteDataStore {
  @Cold
  Single<List<Project>> loadProjectSummaries(User user);

  /**
   * Loads the project with the specified id from the remote data store. The return Single fails
   * with if the project is not found, or if the remote data store is not available.
   */
  @Cold
  Single<Project> loadProject(String projectId);

  /**
   * Returns all features in the specified project, then continues to emit any remote updates to the
   * set of features in the project until all subscribers have been disposed.
   */
  @Cold(stateful = true, terminates = false)
  Flowable<RemoteDataEvent<Feature>> loadFeaturesOnceAndStreamChanges(Project project);

  /**
   * Returns a list of all observations associated with the specified feature, or an empty list if
   * none are found.
   */
  @Cold
  Single<ImmutableList<ValueOrError<Observation>>> loadObservations(Feature feature);

  /**
   * Applies the provided mutations to the remote data store in a single batched transaction. If one
   * update fails, none of the mutations will be applied.
   */
  @Cold
  Completable applyMutations(@Nullable ImmutableCollection<Mutation> mutations, User user);
}
