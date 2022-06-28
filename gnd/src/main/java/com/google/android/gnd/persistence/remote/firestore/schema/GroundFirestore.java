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

package com.google.android.gnd.persistence.remote.firestore.schema;

import com.google.android.gnd.persistence.remote.firestore.base.FluentFirestore;
import com.google.firebase.firestore.FirebaseFirestore;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Object representation of Ground Firestore database. */
@Singleton
public class GroundFirestore extends FluentFirestore {
  private static final String SURVEYS = "surveys";
  private static final String CONFIG = "config";

  @Inject
  GroundFirestore(FirebaseFirestore db) {
    super(db);
  }

  public SurveysCollectionReference surveys() {
    return new SurveysCollectionReference(db().collection(SURVEYS));
  }

  public TermsOfServiceCollectionReference termsOfService() {
    return new TermsOfServiceCollectionReference(db().collection(CONFIG));
  }
}
