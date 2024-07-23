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
import com.google.android.ground.persistence.remote.DataStoreException
import com.google.android.ground.persistence.remote.firebase.protobuf.parseFrom
import com.google.android.ground.persistence.remote.firebase.schema.GeometryConverter.toGeometry
import com.google.android.ground.proto.LocationOfInterest as LocationOfInterestProto
import com.google.android.ground.proto.LocationOfInterest.Source
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

  @Suppress("CognitiveComplexMethod", "LongMethod")
  private fun toLoiUnchecked(survey: Survey, doc: DocumentSnapshot): LocationOfInterest {
    if (!doc.exists()) throw DataStoreException("LOI missing")
    val loiId = doc.id
    val loiProto = LocationOfInterestProto::class.parseFrom(doc, 1)
    // TODO(#2468): Remove this.
    val loiDoc = doc.toObject(LoiDocument::class.java)
    val geometry =
      if (loiProto.geometry != null) {
        loiProto.geometry.toGeometry().getOrThrow()
      } else {
        val geometryMap = DataStoreException.checkNotNull(loiDoc?.geometry, "geometry")
        GeometryConverter.fromFirestoreMap(geometryMap).getOrThrow()
      }
    val jobId = loiProto.jobId.ifEmpty { DataStoreException.checkNotNull(loiDoc?.jobId, JOB_ID) }
    val job = DataStoreException.checkNotNull(survey.getJob(jobId), "job $jobId")
    // Degrade gracefully when audit info missing in remote db.
    val created =
      if (loiProto.created != null) {
        AuditInfoConverter.toAuditInfo(loiProto.created)
      } else {
        AuditInfoConverter.toAuditInfo(loiDoc?.created ?: AuditInfoNestedObject.FALLBACK_VALUE)
      }
    val lastModified =
      if (loiProto.lastModified != null) {
        AuditInfoConverter.toAuditInfo(loiProto.lastModified)
      } else if (loiDoc?.lastModified != null) {
        AuditInfoConverter.toAuditInfo(loiDoc.lastModified)
      } else {
        created
      }
    val submissionCount =
      if (loiProto.submissionCount > 0) loiProto.submissionCount else loiDoc?.submissionCount ?: 0

    val properties =
      if (loiProto.propertiesMap.isNotEmpty()) {
        loiProto.propertiesMap.entries.associate {
          val propertyValue =
            if (it.value.hasNumericValue()) {
              it.value.numericValue
            } else {
              it.value.stringValue
            }
          it.key to propertyValue
        }
      } else {
        loiDoc?.properties ?: mapOf()
      }
    val isPredefined =
      if (loiProto.source != Source.UNRECOGNIZED) {
        loiProto.source == Source.IMPORTED
      } else {
        loiDoc?.predefined
      }
    return LocationOfInterest(
      id = loiId,
      surveyId = survey.id,
      customId = loiProto.customTag.ifEmpty { loiDoc?.customId ?: "" },
      job = job,
      created = created,
      lastModified = lastModified,
      // TODO(#929): Set geometry once LOI has been updated to use our own model.
      geometry = geometry,
      submissionCount = submissionCount,
      ownerEmail = null,
      properties = properties,
      isPredefined = isPredefined,
    )
  }
}
