/*
 * Copyright 2023 Google LLC
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

import com.google.android.ground.model.submission.LocationTaskData
import com.google.android.ground.persistence.local.room.converter.GeometryWrapperTypeConverter
import org.json.JSONObject

/**
 * Converts between `LocationTaskData` and it's equivalent remote representation using JSON
 * representation.
 */
object LocationTaskDataConverter {
  private const val ACCURACY_KEY = "accuracy"
  private const val ALTITUDE_KEY = "altitude"
  private const val GEOMETRY_KEY = "geometry"

  fun toFirestoreMap(taskData: LocationTaskData): Result<Map<String, Any>> =
    Result.runCatching {
      mapOf(
        ACCURACY_KEY to taskData.accuracy!!,
        ALTITUDE_KEY to taskData.altitude!!,
        GEOMETRY_KEY to GeometryConverter.toFirestoreMap(taskData.geometry!!).getOrThrow()
      )
    }

  fun fromFirestoreMap(map: Map<String, *>?): Result<LocationTaskData> =
    Result.runCatching {
      val accuracy = map?.get(ACCURACY_KEY) as? Double
      val altitude = map?.get(ALTITUDE_KEY) as? Double
      val geometry = GeometryConverter.fromFirestoreMap(map?.get(GEOMETRY_KEY) as Map<String, *>?)
      LocationTaskData(geometry.getOrThrow(), altitude, accuracy)
    }

  fun fromJsonObject(data: JSONObject): Result<LocationTaskData> =
    Result.runCatching {
      val accuracy = data.getDouble(ACCURACY_KEY)
      val altitude = data.getDouble(ALTITUDE_KEY)
      val geometry =
        GeometryWrapperTypeConverter.fromString(data.getString(GEOMETRY_KEY))?.getGeometry()
      LocationTaskData(geometry, accuracy, altitude)
    }
}
