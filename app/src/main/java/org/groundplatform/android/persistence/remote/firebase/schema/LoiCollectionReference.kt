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

package org.groundplatform.android.persistence.remote.firebase.schema

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.persistence.remote.firebase.base.FluentCollectionReference
import org.groundplatform.android.persistence.remote.firebase.schema.LoiConverter.toLoi
import org.groundplatform.android.proto.LocationOfInterest as LocationOfInterestProto
import timber.log.Timber

/**
 * Path of field on LOI documents used to differentiate LOIs defined by the organizer vs by data
 * collectors.
 */
const val SOURCE_FIELD = LocationOfInterestProto.SOURCE_FIELD_NUMBER.toString()
/** Path of field on LOI documents representing the creator of the LOI. */
const val OWNER_FIELD = LocationOfInterestProto.OWNER_ID_FIELD_NUMBER.toString()

class LoiCollectionReference internal constructor(ref: CollectionReference) :
  FluentCollectionReference(ref) {

  fun loi(id: String) = LoiDocumentReference(reference().document(id))

  /** Retrieves all "predefined" LOIs in the specified survey. Main-safe. */
  suspend fun fetchPredefined(survey: Survey): List<LocationOfInterest> =
    // Use !=false rather than ==true to not break legacy dev surveys.
    // TODO: Switch to whereEqualTo(true) once legacy dev surveys deleted or migrated.
    // Issue URL: https://github.com/google/ground-android/issues/2375
    fetchLois(
      survey,
      reference().whereEqualTo(SOURCE_FIELD, LocationOfInterestProto.Source.IMPORTED.number),
    )

  /** Retrieves LOIs created by the specified email in the specified survey. Main-safe. */
  suspend fun fetchUserDefined(survey: Survey, ownerUserId: String): List<LocationOfInterest> =
    fetchLois(
      survey,
      reference()
        .whereEqualTo(SOURCE_FIELD, LocationOfInterestProto.Source.FIELD_DATA.number)
        .whereEqualTo(OWNER_FIELD, ownerUserId),
    )

  private suspend fun fetchLois(survey: Survey, query: Query): List<LocationOfInterest> =
    try {
      val snapshot = query.get().await()
      snapshot.documents.mapNotNull {
        toLoi(survey, it)
          .onFailure { t -> Timber.e(t, "Invalid LOI ${it.id} in remote survey ${survey.id}") }
          .getOrNull()
      }
    } catch (e: CancellationException) {
      Timber.i(e, "Fetching LOIs was cancelled")
      listOf()
    }
}
