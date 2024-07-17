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
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.firebase.firestore.DocumentSnapshot

// TODO: Add tests.
/** Converts between Firestore documents and [LocationOfInterest] instances. */
object LoiConverter {
  // TODO(#2392): Define field names on DocumentReference objects, not converters.
  private const val JOB_ID = "jobId"
  const val GEOMETRY_TYPE = "type"
  const val POLYGON_TYPE = "Polygon"

  fun toLoi(survey: Survey, doc: DocumentSnapshot): Result<LocationOfInterest> = runCatching {
    toLoiUnchecked(survey, doc)
  }

  private fun toLoiUnchecked(survey: Survey, doc: DocumentSnapshot): LocationOfInterest {
    if (!doc.exists()) throw DataStoreException("LOI missing")
    val loiId = doc.id
    val loiDoc =
      DataStoreException.checkNotNull(doc.toObject(LoiDocument::class.java), "loi document")
    val geometryMap = DataStoreException.checkNotNull(loiDoc.geometry, "geometry")
    val geometry = GeometryConverter.fromFirestoreMap(geometryMap).getOrThrow()

    return createLocationOfInterest(survey, loiId, loiDoc, geometry)
  }

  private fun createLocationOfInterest(
    survey: Survey,
    loiId: String,
    loiDoc: LoiDocument,
    geometry: Geometry,
  ): LocationOfInterest {
    val jobId = DataStoreException.checkNotNull(loiDoc.jobId, JOB_ID)
    val job = DataStoreException.checkNotNull(survey.getJob(jobId), "job ${loiDoc.jobId}")
    // Degrade gracefully when audit info missing in remote db.
    val created = loiDoc.created ?: AuditInfoNestedObject.FALLBACK_VALUE
    val lastModified = loiDoc.lastModified ?: created
    val submissionCount = loiDoc.submissionCount ?: 0
    return LocationOfInterest(
      id = loiId,
      surveyId = survey.id,
      customId = loiDoc.customId ?: "",
      job = job,
      created = AuditInfoConverter.toAuditInfo(created),
      lastModified = AuditInfoConverter.toAuditInfo(lastModified),
      // TODO(#929): Set geometry once LOI has been updated to use our own model.
      geometry = geometry,
      submissionCount = submissionCount,
      properties = loiDoc.properties ?: mapOf(),
      isPredefined = loiDoc.predefined,
    )
  }
}
