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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.persistence.remote.firebase.base.FluentCollectionReference
import com.google.android.ground.persistence.remote.firebase.schema.LoiConverter.toLoi
import com.google.android.ground.rx.annotations.Cold
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.QuerySnapshot
import durdinapps.rxfirebase2.RxFirestore
import io.reactivex.Single
import timber.log.Timber

class LoiCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {

  /** Retrieves all LOIs in the specified survey. */
  fun locationsOfInterest(
    survey: Survey
  ): @Cold(terminates = false) Single<List<LocationOfInterest>> =
    RxFirestore.getCollection(reference())
      .map { snapshot: QuerySnapshot -> toLois(survey, snapshot) }
      .toSingle(emptyList())

  private fun toLois(survey: Survey, snapshot: QuerySnapshot): List<LocationOfInterest> =
    snapshot.documents
      .map { toLoi(survey, it) }
      .mapNotNull { it.onFailure { t -> Timber.d(t) }.getOrNull() }

  fun loi(id: String) = LoiDocumentReference(reference().document(id))
}
