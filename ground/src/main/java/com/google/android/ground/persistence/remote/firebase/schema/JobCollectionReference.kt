/*
 * Copyright 2024 Google LLC
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

import com.google.android.ground.model.job.Job
import com.google.android.ground.persistence.remote.firebase.base.FluentCollectionReference
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class JobCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {
  fun get(): Flow<List<Job>> = callbackFlow {
    reference()
      .get()
      .addOnSuccessListener { trySend(it.documents.map { doc -> JobConverter.toJob(doc) }) }
      .addOnFailureListener { trySend(listOf()) }

    awaitClose {
      // Cannot cancel or detach listeners here.
    }
  }
}
