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

import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.remote.firestore.base.FluentCollectionReference
import com.google.android.ground.rx.annotations.Cold
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import com.google.firebase.firestore.*
import durdinapps.rxfirebase2.RxFirestore
import io.reactivex.Single

class SubmissionCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {

  fun submission(id: String) = SubmissionDocumentReference(reference().document(id))

  fun submissionsByLocationOfInterestId(
    locationOfInterest: LocationOfInterest
  ): @Cold Single<ImmutableList<Result<Submission>>> {
    return RxFirestore.getCollection(byLoiId(locationOfInterest.id))
      .map { querySnapshot: QuerySnapshot -> convert(querySnapshot, locationOfInterest) }
      .toSingle(ImmutableList.of())
  }

  private fun convert(
    querySnapshot: QuerySnapshot,
    locationOfInterest: LocationOfInterest
  ): ImmutableList<Result<Submission>> {
    return querySnapshot.documents
      .map { doc: DocumentSnapshot ->
        runCatching { SubmissionConverter.toSubmission(locationOfInterest, doc) }
      }
      .toImmutableList()
  }

  private fun byLoiId(loiId: String): Query =
    reference().whereEqualTo(FieldPath.of(SubmissionMutationConverter.LOI_ID), loiId)
}
