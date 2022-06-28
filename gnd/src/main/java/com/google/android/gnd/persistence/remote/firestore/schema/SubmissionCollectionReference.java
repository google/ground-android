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

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.submission.Submission;
import com.google.android.gnd.persistence.remote.firestore.base.FluentCollectionReference;
import com.google.android.gnd.rx.ValueOrError;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Single;
import org.jetbrains.annotations.NotNull;

public class SubmissionCollectionReference extends FluentCollectionReference {

  SubmissionCollectionReference(CollectionReference ref) {
    super(ref);
  }

  public SubmissionDocumentReference submission(String id) {
    return new SubmissionDocumentReference(reference().document(id));
  }

  @Cold
  public Single<ImmutableList<ValueOrError<Submission>>> submissionsByLoiId(Feature feature) {
    return RxFirestore.getCollection(byLoiId(feature.getId()))
        .map(querySnapshot -> convert(querySnapshot, feature))
        .toSingle(ImmutableList.of());
  }

  @NotNull
  private ImmutableList<ValueOrError<Submission>> convert(
      QuerySnapshot querySnapshot, Feature feature) {
    return stream(querySnapshot.getDocuments())
        .map(doc -> ValueOrError.create(() -> SubmissionConverter.toSubmission(feature, doc)))
        .collect(toImmutableList());
  }

  private Query byLoiId(String loiId) {
    return reference().whereEqualTo(FieldPath.of(SubmissionMutationConverter.LOI_ID), loiId);
  }
}
