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

import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.persistence.local.LocalDataStoreConverter
import com.google.android.ground.persistence.local.room.entity.LocationOfInterestMutationEntity
import com.google.android.ground.persistence.local.room.models.MutationEntitySyncStatus
import com.google.android.ground.persistence.local.room.models.MutationEntityType
import java.util.*

/**
 * Converts [LocationOfInterestMutation] model objects to/from a persisted database representation.
 */
object LocationOfInterestMutationConverter :
  LocalDataStoreConverter<LocationOfInterestMutation, LocationOfInterestMutationEntity> {
  override fun convertToDataStoreObject(
    model: LocationOfInterestMutation
  ): LocationOfInterestMutationEntity =
    LocationOfInterestMutationEntity(
      id = model.id,
      surveyId = model.surveyId,
      jobId = model.jobId,
      type = MutationEntityType.fromMutationType(model.type),
      newGeometry = model.geometry?.let { GeometryConverter.convertToDataStoreObject(it) },
      userId = model.userId,
      locationOfInterestId = model.locationOfInterestId,
      syncStatus = MutationEntitySyncStatus.fromMutationSyncStatus(model.syncStatus),
      clientTimestamp = model.clientTimestamp.time,
      lastError = model.lastError,
      retryCount = model.retryCount
    )

  override fun convertFromDataStoreObject(
    entity: LocationOfInterestMutationEntity
  ): LocationOfInterestMutation =
    LocationOfInterestMutation(
      id = entity.id,
      surveyId = entity.surveyId,
      jobId = entity.jobId,
      type = entity.type.toMutationType(),
      geometry = entity.newGeometry?.let { GeometryConverter.convertFromDataStoreObject(it) },
      userId = entity.userId,
      locationOfInterestId = entity.locationOfInterestId,
      syncStatus = entity.syncStatus.toMutationSyncStatus(),
      clientTimestamp = Date(entity.clientTimestamp),
      lastError = entity.lastError,
      retryCount = entity.retryCount,
    )
}
