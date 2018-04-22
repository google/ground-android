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

package com.google.android.gnd.service;

import android.support.annotation.Nullable;
import com.google.android.gnd.model.Feature;
import com.google.android.gnd.model.FeatureUpdate;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.Record;
import io.reactivex.Flowable;
import java.util.List;
import java8.util.concurrent.CompletableFuture;

/**
 * Data service is also responsible for caching data locally and persisting data for when network
 * connection is unavailable.
 */
public interface DataService {
  void onCreate();

  CompletableFuture<Project> loadProject(String projectId);

  Feature update(String projectId, FeatureUpdate featureUpdate);

  CompletableFuture<List<Record>> loadRecordData(String projectId, String featureId);

  CompletableFuture<List<Project>> getProjectSummaries();

  Flowable<DatastoreEvent<Feature>> observePlaces(String projectId);

  interface DataChangeListener<T> {
    void onChange(String id, @Nullable T obj, ChangeType changeType, boolean hasPendingWrites);

    enum ChangeType {
      UNKNOWN,
      ADDED,
      MODIFIED,
      REMOVED
    }
  }
}
