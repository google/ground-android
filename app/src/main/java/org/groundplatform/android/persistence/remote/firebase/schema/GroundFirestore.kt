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
package org.groundplatform.android.persistence.remote.firebase.schema

import com.google.firebase.firestore.FirebaseFirestore
import org.groundplatform.android.persistence.remote.firebase.base.FluentFirestore

/** Object representation of Ground Firestore database. */
class GroundFirestore(db: FirebaseFirestore) : FluentFirestore(db) {
  fun surveys(): SurveysCollectionReference = SurveysCollectionReference(db().collection(SURVEYS))

  fun termsOfService(): TermsOfServiceCollectionReference =
    TermsOfServiceCollectionReference(db().collection(CONFIG))

  companion object {
    private const val SURVEYS = "surveys"
    private const val CONFIG = "config"
  }
}
