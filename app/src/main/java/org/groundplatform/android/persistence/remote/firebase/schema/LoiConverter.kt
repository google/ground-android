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

import com.google.firebase.firestore.DocumentSnapshot
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.persistence.remote.DataStoreException
import org.groundplatform.android.persistence.remote.firebase.protobuf.parseFrom
import org.groundplatform.android.persistence.remote.firebase.schema.GeometryConverter.toGeometry
import org.groundplatform.android.proto.LocationOfInterest as LocationOfInterestProto
import org.groundplatform.android.proto.LocationOfInterest.Source

/** Converts between Firestore documents and [LocationOfInterest] instances. */
object LoiConverter {
  // TODO: Define field names on DocumentReference objects, not converters.
  // Issue URL: https://github.com/google/ground-android/issues/2375
  const val GEOMETRY_TYPE = "type"
  const val POLYGON_TYPE = "Polygon"

  fun toLoi(survey: Survey, doc: DocumentSnapshot): Result<LocationOfInterest> = runCatching {
    toLoiUnchecked(survey, doc)
  }

  @Suppress("CognitiveComplexMethod", "LongMethod")
  private fun toLoiUnchecked(survey: Survey, doc: DocumentSnapshot): LocationOfInterest {
    if (!doc.exists()) throw DataStoreException("LOI missing")
    val loiId = doc.id
    val loiProto = LocationOfInterestProto::class.parseFrom(doc, 1)
    val geometry = loiProto.geometry.toGeometry()
    val jobId = loiProto.jobId
    val job = DataStoreException.checkNotNull(survey.getJob(jobId), "job $jobId")
    // Degrade gracefully when audit info missing in remote db.
    val created = AuditInfoConverter.toAuditInfo(loiProto.created)
    val lastModified =
      if (loiProto.lastModified != null) {
        AuditInfoConverter.toAuditInfo(loiProto.lastModified)
      } else {
        created
      }
    val submissionCount = loiProto.submissionCount

    val properties =
      loiProto.propertiesMap.entries.associate {
        val propertyValue =
          if (it.value.hasNumericValue()) {
            it.value.numericValue
          } else {
            it.value.stringValue
          }
        it.key to propertyValue
      }
    val isPredefined = loiProto.source == Source.IMPORTED
    return LocationOfInterest(
      id = loiId,
      surveyId = survey.id,
      customId = loiProto.customTag,
      job = job,
      created = created,
      lastModified = lastModified,
      // TODO: Set geometry once LOI has been updated to use our own model.
      // Issue URL: https://github.com/google/ground-android/issues/929
      geometry = geometry,
      submissionCount = submissionCount,
      properties = properties,
      isPredefined = isPredefined,
    )
  }
}
