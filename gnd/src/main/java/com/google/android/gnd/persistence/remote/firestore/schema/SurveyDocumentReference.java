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

import com.google.android.gnd.model.Survey;
import com.google.android.gnd.persistence.remote.firestore.base.FluentDocumentReference;
import com.google.firebase.firestore.DocumentReference;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Maybe;

public class SurveyDocumentReference extends FluentDocumentReference {
  private static final String LOIS = "lois";
  private static final String SUBMISSIONS = "submissions";

  SurveyDocumentReference(DocumentReference ref) {
    super(ref);
  }

  public LoiCollectionReference lois() {
    return new LoiCollectionReference(reference().collection(LOIS));
  }

  public SubmissionCollectionReference submissions() {
    return new SubmissionCollectionReference(reference().collection(SUBMISSIONS));
  }

  public Maybe<Survey> get() {
    return RxFirestore.getDocument(reference()).map(SurveyConverter::toSurvey);
  }
}
