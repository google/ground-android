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
import com.google.android.gnd.persistence.remote.firestore.FeatureDoc;
import com.google.android.gnd.persistence.remote.firestore.base.FluentCollectionReference;
import com.google.android.gnd.persistence.remote.firestore.converters.QuerySnapshotConverter;
import com.google.firebase.firestore.CollectionReference;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Flowable;

public class FeaturesCollectionReference extends FluentCollectionReference {
  FeaturesCollectionReference(CollectionReference ref) {
    super(ref);
  }

  public FeatureDocumentReference feature(String id) {
    return new FeatureDocumentReference(reference().document(id));
  }

  public Flowable<RemoteDataEvent<Feature>> observe(Project project) {
    return RxFirestore.observeQueryRef(reference())
        .flatMapIterable(
            featureQuerySnapshot ->
                QuerySnapshotConverter.toEvents(
                    featureQuerySnapshot,
                    featureDocSnapshot -> FeatureDoc.toObject(project, featureDocSnapshot)));
  }
}
