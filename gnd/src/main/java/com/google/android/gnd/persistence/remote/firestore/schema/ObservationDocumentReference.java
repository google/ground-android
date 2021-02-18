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

import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.persistence.remote.firestore.base.FluentDocumentReference;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Maybe;

public class ObservationDocumentReference extends FluentDocumentReference {
  ObservationDocumentReference(DocumentReference ref) {
    super(ref);
  }

  @Cold
  public Maybe<Observation> get(Feature feature) {
    return RxFirestore.getDocument(reference())
        .map(doc -> ObservationConverter.toObservation(feature, doc));
  }

  /** Appends the operation described by the specified mutation to the provided write batch. */
  public void addMutationToBatch(ObservationMutation mutation, User user, WriteBatch batch) {
    switch (mutation.getType()) {
      case CREATE:
      case UPDATE:
        merge(ObservationMutationConverter.toMap(mutation, user), batch);
        break;
      case DELETE:
        delete(batch);
        break;
      default:
        throw new IllegalArgumentException("Unknown mutation type " + mutation.getType());
    }
  }
}
