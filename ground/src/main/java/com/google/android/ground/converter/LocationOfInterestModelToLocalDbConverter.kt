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
package com.google.android.ground.converter

import com.google.android.ground.model.AuditInfo
import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.local.room.entity.AuditInfoEntity
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestEntity
import com.google.android.ground.persistence.local.room.models.EntityState

/**
 * Converts [LocationOfInterest] model objects associated with a given [Survey] to/from a persisted
 * database representation.
 */
class LocationOfInterestModelToLocalDbConverter(val survey: Survey) :
  Converter<LocationOfInterest, LocationOfInterestEntity> {

  companion object : Converter<LocationOfInterest, LocationOfInterestEntity> {
    fun fromMutation(
      mutation: LocationOfInterestMutation,
      created: AuditInfo
    ): LocationOfInterestEntity {
      val authInfo = AuditInfoEntity.fromObject(created)
      return LocationOfInterestEntity(
        id = mutation.locationOfInterestId,
        surveyId = mutation.surveyId,
        jobId = mutation.jobId,
        state = EntityState.DEFAULT,
        created = authInfo,
        lastModified = authInfo,
        geometry = mutation.geometry?.let { GeometryModelToLocalDbConverter.convertTo(it) }
      )
    }

    // Permits serialization w/o having to pass a `survey`.
    override fun convertTo(model: LocationOfInterest): LocationOfInterestEntity =
      LocationOfInterestEntity(
        id = model.id,
        surveyId = model.surveyId,
        jobId = model.job.id,
        state = EntityState.DEFAULT,
        created = AuditInfoEntity.fromObject(model.created),
        lastModified = AuditInfoEntity.fromObject(model.lastModified),
        geometry = GeometryModelToLocalDbConverter.convertTo(model.geometry)
      )

    override fun convertFrom(entity: LocationOfInterestEntity): LocationOfInterest? = null
  }

  // We can serialize perfectly fine without passing a `survey`; delegate to the companion.
  override fun convertTo(model: LocationOfInterest): LocationOfInterestEntity =
    Companion.convertTo(model)

  override fun convertFrom(entity: LocationOfInterestEntity): LocationOfInterest {
    val geometry = entity.geometry?.let { GeometryModelToLocalDbConverter.convertFrom(it) }
    if (geometry == null) {
      throw LocalDataConsistencyException("No geometry in location of interest $entity.id")
    } else {
      return LocationOfInterest(
        id = entity.id,
        surveyId = entity.surveyId,
        created = AuditInfoEntity.toObject(entity.created),
        lastModified = AuditInfoEntity.toObject(entity.lastModified),
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
