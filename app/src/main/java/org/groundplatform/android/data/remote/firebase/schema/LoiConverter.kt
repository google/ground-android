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

import com.google.firebase.firestore.DocumentSnapshot
import org.groundplatform.android.data.remote.DataStoreException
import org.groundplatform.android.data.remote.firebase.protobuf.parseFrom
import org.groundplatform.android.proto.LocationOfInterest as LocationOfInterestProto
import org.groundplatform.android.proto.LocationOfInterest.Source
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.locationofinterest.LOI_ID_PROPERTY
import org.groundplatform.domain.model.locationofinterest.LOI_NAME_PROPERTY
import org.groundplatform.domain.model.locationofinterest.LocationOfInterest

/** Converts between Firestore documents and [LocationOfInterest] instances. */
object LoiConverter {
  private val GEOMETRY_FIELD = LocationOfInterestProto.GEOMETRY_FIELD_NUMBER.toString()
  private val PROPERTIES_FIELD = LocationOfInterestProto.PROPERTIES_FIELD_NUMBER.toString()
  private val PROPERTY_STRING_VALUE =
    LocationOfInterestProto.Property.STRING_VALUE_FIELD_NUMBER.toString()
  private val PROPERTY_NUMERIC_VALUE =
    LocationOfInterestProto.Property.NUMERIC_VALUE_FIELD_NUMBER.toString()

  private val RETAINED_PROPERTIES = listOf(LOI_NAME_PROPERTY, LOI_ID_PROPERTY)

  fun toLoi(survey: Survey, doc: DocumentSnapshot): Result<LocationOfInterest> = runCatching {
    toLoiUnchecked(survey, doc)
  }

  @Suppress("CognitiveComplexMethod", "LongMethod")
  private fun toLoiUnchecked(survey: Survey, doc: DocumentSnapshot): LocationOfInterest {
    if (!doc.exists()) throw DataStoreException("LOI missing")
    val loiId = doc.id
    val data = doc.data.orEmpty()
    val geometry = LoiGeometryConverter.toGeometry(data[GEOMETRY_FIELD])
    val properties = pruneUnusedProperties(data[PROPERTIES_FIELD])
    val loiProto =
      LocationOfInterestProto::class.parseFrom(loiId, data - GEOMETRY_FIELD - PROPERTIES_FIELD, 1)
    val jobId = loiProto.jobId
    val job = DataStoreException.checkNotNull(survey.getJob(jobId), "job $jobId")
    // Degrade gracefully when audit info missing in remote db.
    val created = AuditInfoConverter.toAuditInfo(loiProto.created)
    val lastModified =
      if (loiProto.hasLastModified()) {
        AuditInfoConverter.toAuditInfo(loiProto.lastModified)
      } else {
        created
      }
    val submissionCount = loiProto.submissionCount

    val isPredefined = loiProto.source == Source.IMPORTED
    return LocationOfInterest(
      id = loiId,
      surveyId = survey.id,
      customId = loiProto.customTag,
      job = job,
      created = created,
      lastModified = lastModified,
      geometry = geometry,
      submissionCount = submissionCount,
      properties = properties,
      isPredefined = isPredefined,
    )
  }

  private fun pruneUnusedProperties(value: Any?): Map<String, Any> {
    val properties = value as? Map<*, *> ?: return mapOf()
    return RETAINED_PROPERTIES.mapNotNull { key ->
        (properties[key] as? Map<*, *>)?.let { property ->
          val numeric = property[PROPERTY_NUMERIC_VALUE] as? Number
          val text = property[PROPERTY_STRING_VALUE] as? String
          (numeric ?: text)?.let { key to it }
        }
      }
      .toMap()
  }
}
