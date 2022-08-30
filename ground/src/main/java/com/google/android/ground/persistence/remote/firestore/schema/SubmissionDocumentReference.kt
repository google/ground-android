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

import com.google.android.ground.model.User
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.persistence.remote.firestore.base.FluentDocumentReference
import com.google.android.ground.rx.annotations.Cold
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.WriteBatch
import durdinapps.rxfirebase2.RxFirestore
import io.reactivex.Maybe

class SubmissionDocumentReference internal constructor(ref: DocumentReference) :
  FluentDocumentReference(ref) {
  operator fun get(locationOfInterest: LocationOfInterest): @Cold Maybe<Submission> {
    return RxFirestore.getDocument(reference()).map { doc: DocumentSnapshot ->
      SubmissionConverter.toSubmission(locationOfInterest, doc)
    }
  }

  /** Appends the operation described by the specified mutation to the provided write batch. */
  fun addMutationToBatch(mutation: SubmissionMutation, user: User, batch: WriteBatch) {
    when (mutation.type) {
      Mutation.Type.CREATE,
      Mutation.Type.UPDATE -> merge(SubmissionMutationConverter.toMap(mutation, user), batch)
      Mutation.Type.DELETE -> delete(batch)
      else -> throw IllegalArgumentException("Unknown mutation type ${mutation.type}")
    }
  }
}
