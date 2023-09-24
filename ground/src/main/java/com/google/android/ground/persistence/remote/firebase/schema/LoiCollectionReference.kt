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
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class LoiCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {

  fun loi(id: String) = LoiDocumentReference(reference().document(id))

  /** Retrieves all LOIs in the specified survey. Main-safe. */
  suspend fun locationsOfInterest(survey: Survey): List<LocationOfInterest> =
    withContext(ioDispatcher) { toLois(survey, reference().get().await()) }

  private suspend fun toLois(survey: Survey, snapshot: QuerySnapshot): List<LocationOfInterest> =
    withContext(defaultDispatcher) {
      snapshot.documents.mapNotNull {
        toLoi(survey, it)
          .onFailure { t ->
            Timber.e(t, "Unable to load loi(${it.id}) for survey(${survey.title}-${survey.id})")
          }
          .getOrNull()
      }
    }
}
