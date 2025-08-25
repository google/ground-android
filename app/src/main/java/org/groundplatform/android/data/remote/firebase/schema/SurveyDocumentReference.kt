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

package org.groundplatform.android.data.remote.firebase.schema

import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.groundplatform.android.data.remote.firebase.base.FluentDocumentReference
import org.groundplatform.android.model.Survey
import timber.log.Timber

private const val LOIS = "lois"
private const val SUBMISSIONS = "submissions"
private const val JOBS = "jobs"

class SurveyDocumentReference internal constructor(ref: DocumentReference) :
  FluentDocumentReference(ref) {

  fun lois() = LoiCollectionReference(reference().collection(LOIS))

  fun submissions() = SubmissionCollectionReference(reference().collection(SUBMISSIONS))

  private fun jobs() = JobCollectionReference(reference().collection(JOBS))

  suspend fun get(): Survey? {
    try {
      val surveyDoc = reference().get().await()
      // TODO: Move jobs fetch to outside this DocumentReference class.
      // Issue URL: https://github.com/google/ground-android/issues/2864
      val jobs = jobs().get()
      return SurveyConverter.toSurvey(surveyDoc, jobs)
    } catch (e: CancellationException) {
      Timber.i(e, "Fetching survey was cancelled")
      return null
    }
  }
}
