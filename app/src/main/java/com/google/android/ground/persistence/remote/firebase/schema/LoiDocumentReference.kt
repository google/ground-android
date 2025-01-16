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

import com.google.android.ground.model.User
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.persistence.remote.firebase.base.FluentDocumentReference
import com.google.android.ground.persistence.remote.firebase.protobuf.createLoiMessage
import com.google.android.ground.persistence.remote.firebase.protobuf.toFirestoreMap
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.WriteBatch

class LoiDocumentReference internal constructor(ref: DocumentReference) :
  FluentDocumentReference(ref) {

  /** Appends the operation described by the specified mutation to the provided write batch. */
  fun addMutationToBatch(mutation: LocationOfInterestMutation, user: User, batch: WriteBatch) =
    when (mutation.type) {
      Mutation.Type.CREATE,
      Mutation.Type.UPDATE -> merge(mutation.createLoiMessage(user).toFirestoreMap(), batch)
      Mutation.Type.DELETE ->
        // The server is expected to do a cascading delete of all submissions for the deleted LOI.
        delete(batch)
      else -> throw IllegalArgumentException("Unknown mutation type ${mutation.type}")
    }
}
