/*
 * Copyright 2021 Google LLC
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

import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.groundplatform.android.model.TermsOfService
import org.groundplatform.android.persistence.remote.firebase.base.FluentDocumentReference
import timber.log.Timber

class TermsOfServiceDocumentReference internal constructor(ref: DocumentReference) :
  FluentDocumentReference(ref) {

  fun terms() = TermsOfServiceDocumentReference(reference())

  suspend fun get(): TermsOfService? {
    try {
      val documentSnapshot = reference().get().await()
      return TermsOfServiceConverter.toTerms(documentSnapshot)
    } catch (e: CancellationException) {
      Timber.i(e, "Fetching TermsOfService was cancelled")
      return null
    }
  }
}
