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
import com.google.android.gnd.persistence.remote.firestore.base.FluentDocumentReference;
import com.google.firebase.firestore.DocumentReference;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Maybe;

public class ProjectDocumentReference extends FluentDocumentReference {
  private static final String FEATURES = "features";
  private static final String OBSERVATIONS = "observations";

  ProjectDocumentReference(DocumentReference ref) {
    super(ref);
  }

  public FeaturesCollectionReference features() {
    return new FeaturesCollectionReference(reference().collection(FEATURES));
  }

  public ObservationsCollectionReference observations() {
    return new ObservationsCollectionReference(reference().collection(OBSERVATIONS));
  }

  public Maybe<Project> get() {
    return RxFirestore.getDocument(reference()).map(ProjectConverter::toProject);
  }
}
