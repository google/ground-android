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

package com.google.android.ground.persistence.remote.firestore.schema

import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.persistence.remote.RemoteDataEvent
import com.google.android.ground.persistence.remote.firestore.base.FluentCollectionReference
import com.google.android.ground.persistence.remote.firestore.schema.LoiConverter.toLoi
import com.google.android.ground.rx.annotations.Cold
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import durdinapps.rxfirebase2.RxFirestore
import io.reactivex.Flowable

class LoiCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {

  /** Retrieves all lois in the survey, then streams changes to the remote db incrementally. */
  fun loadOnceAndStreamChanges(
    survey: Survey
  ): @Cold(terminates = false) Flowable<RemoteDataEvent<LocationOfInterest>> =
    RxFirestore.observeQueryRef(reference()).flatMapIterable { snapshot: QuerySnapshot ->
      toRemoteDataEvents(survey, snapshot)
    }

  fun loi(id: String) = LoiDocumentReference(reference().document(id))

  private fun toRemoteDataEvents(
    survey: Survey,
    snapshot: QuerySnapshot
  ): Iterable<RemoteDataEvent<LocationOfInterest>> =
    QuerySnapshotConverter.toEvents(snapshot) { doc: DocumentSnapshot -> toLoi(survey, doc) }
}
