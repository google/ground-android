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

import com.google.android.ground.FirebaseCrashLogger
import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.persistence.remote.firebase.base.FluentCollectionReference
import com.google.android.ground.persistence.remote.firebase.schema.LoiConverter.toLoi
import com.google.android.ground.proto.LocationOfInterest as LocationOfInterestProto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
    withContext(ioDispatcher) {
      // Use !=false rather than ==true to not break legacy dev surveys.
      // TODO(#2375): Switch to whereEqualTo(true) once legacy dev surveys deleted or migrated.
      val query =
        reference().whereEqualTo(SOURCE_FIELD, LocationOfInterestProto.Source.IMPORTED.number)
      toLois(survey, query.get().await())
    }

  /** Retrieves LOIs created by the specified email in the specified survey. Main-safe. */
  suspend fun fetchUserDefined(survey: Survey, ownerUserId: String): List<LocationOfInterest> =
    withContext(ioDispatcher) {
      val query =
        reference()
          .whereEqualTo(SOURCE_FIELD, LocationOfInterestProto.Source.FIELD_DATA.number)
          .whereEqualTo(OWNER_FIELD, ownerUserId)
      toLois(survey, query.get().await())
    }

  private suspend fun toLois(survey: Survey, snapshot: QuerySnapshot): List<LocationOfInterest> =
    withContext(defaultDispatcher) {
      snapshot.documents.mapNotNull {
        toLoi(survey, it)
          .onFailure { t ->
            val logger = FirebaseCrashLogger()
            logger.setSelectedSurveyId(survey.id)
            logger.logException(t)
            Timber.w(t, "LOI ${it.id} in remote survey ${survey.id} is invalid")
          }
          .getOrNull()
      }
    }
}
