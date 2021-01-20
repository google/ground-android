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

package com.google.android.gnd.persistence.remote.firestore.schema;

import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.firestore.base.FluentCollectionReference;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QuerySnapshot;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Flowable;

public class FeaturesCollectionReference extends FluentCollectionReference {
  FeaturesCollectionReference(CollectionReference ref) {
    super(ref);
  }

  public FeatureDocumentReference feature(String id) {
    return new FeatureDocumentReference(reference().document(id));
  }

  /** Retrieves all features in the project, then streams changes to the remote db incrementally. */
  @Cold(terminates = false)
  public Flowable<RemoteDataEvent<Feature>> loadOnceAndStreamChanges(Project project) {
    return RxFirestore.observeQueryRef(reference())
        .flatMapIterable(snapshot -> toRemoteDataEvents(project, snapshot));
  }

  private static Iterable<RemoteDataEvent<Feature>> toRemoteDataEvents(
      Project project, QuerySnapshot snapshot) {
    return QuerySnapshotConverter.toEvents(
        snapshot, doc -> FeatureConverter.toFeature(project, doc));
  }
}
