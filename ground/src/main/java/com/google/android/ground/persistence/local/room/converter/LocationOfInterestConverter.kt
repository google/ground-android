/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.persistence.local.room.converter

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.local.LocalDataStoreConverter
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestEntity
import com.google.android.ground.persistence.local.room.models.EntityState

/**
 * Converts [LocationOfInterest] model objects associated with a given [Survey] to/from a persisted
 * database representation.
 */
// TODO: Change this to a singleton object. We only need a class to encapsulate the `survey` we'll
// use to deserialize LOI jobs. Instead, we should only store job ids in LOIs and simplify this
// implementation.
class LocationOfInterestConverter(val survey: Survey) :
  LocalDataStoreConverter<LocationOfInterest, LocationOfInterestEntity> {

  // TODO: Remove once we store job ids in LocationOfInterest
  companion object {
    fun fromMutation(
      mutation: LocationOfInterestMutation,
      created: AuditInfo
    ): LocationOfInterestEntity {
      val authInfo = AuditInfoConverter.convertToDataStoreObject(created)
      return LocationOfInterestEntity(
        id = mutation.locationOfInterestId,
        surveyId = mutation.surveyId,
        jobId = mutation.jobId,
        state = EntityState.DEFAULT,
        created = authInfo,
        lastModified = authInfo,
        geometry = mutation.geometry?.let { GeometryConverter.convertToDataStoreObject(it) }
      )
    }

    // Permits serialization w/o having to pass a `survey`.
    fun convertTo(model: LocationOfInterest): LocationOfInterestEntity =
      LocationOfInterestEntity(
        id = model.id,
        surveyId = model.surveyId,
        jobId = model.job.id,
        state = EntityState.DEFAULT,
        created = AuditInfoConverter.convertToDataStoreObject(model.created),
        lastModified = AuditInfoConverter.convertToDataStoreObject(model.lastModified),
        geometry = GeometryConverter.convertToDataStoreObject(model.geometry)
      )
  }

  // We can serialize perfectly fine without passing a `survey`; delegate to the companion.
  override fun convertToDataStoreObject(model: LocationOfInterest): LocationOfInterestEntity =
    convertTo(model)

  override fun convertFromDataStoreObject(entity: LocationOfInterestEntity): LocationOfInterest {
    val geometry = entity.geometry?.let { GeometryConverter.convertFromDataStoreObject(it) }
    if (geometry == null) {
      throw LocalDataConsistencyException("No geometry in location of interest $entity.id")
    } else {
      return LocationOfInterest(
        id = entity.id,
        surveyId = entity.surveyId,
        created = AuditInfoConverter.convertFromDataStoreObject(entity.created),
        lastModified = AuditInfoConverter.convertFromDataStoreObject(entity.lastModified),
        geometry = geometry,
        job =
          this.survey.getJob(jobId = entity.jobId).orElseThrow {
            LocalDataConsistencyException(
              "Unknown jobId $entity.jobId in location of interest $entity.id"
            )
          }
      )
    }
  }
}
