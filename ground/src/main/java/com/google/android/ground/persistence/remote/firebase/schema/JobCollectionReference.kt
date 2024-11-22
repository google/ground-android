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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class JobCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {
  suspend fun get(): List<Job> =
    withContext(ioDispatcher) {
      val docs = reference().get().await()
      docs.map { doc -> JobConverter.toJob(doc) }
    }
}
