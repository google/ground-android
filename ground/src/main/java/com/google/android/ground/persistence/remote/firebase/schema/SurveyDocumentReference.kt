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
import com.google.android.ground.persistence.remote.firebase.base.FluentDocumentReference
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.tasks.await

private const val LOIS = "lois"
private const val SUBMISSIONS = "submissions"
private const val JOBS = "jobs"

class SurveyDocumentReference internal constructor(ref: DocumentReference) :
  FluentDocumentReference(ref) {

  fun lois() = LoiCollectionReference(reference().collection(LOIS))

  fun submissions() = SubmissionCollectionReference(reference().collection(SUBMISSIONS))

  private fun jobs() = JobCollectionReference(reference().collection(JOBS))

  suspend fun get(): Survey {
    val document = reference().get().await()
    val jobs = jobs().get().firstOrNull() ?: listOf()
    return SurveyConverter.toSurvey(document, jobs)
  }
}
