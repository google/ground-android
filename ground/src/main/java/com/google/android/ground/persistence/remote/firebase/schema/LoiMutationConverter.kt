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

import com.google.android.ground.model.User
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.persistence.remote.firebase.schema.AuditInfoConverter.fromMutationAndUser
import com.google.firebase.firestore.GeoPoint
import kotlinx.collections.immutable.toPersistentMap

/**
 * Converts between Firestore maps used to merge updates and [LocationOfInterestMutation] instances.
 */
internal object LoiMutationConverter {

  /**
   * Returns a map containing key-value pairs usable by Firestore constructed from the provided
   * mutation.
   */
  fun toMap(mutation: LocationOfInterestMutation, user: User): Map<String, Any> {
    val map = mutableMapOf<String, Any>()

    map[LoiConverter.JOB_ID] = mutation.jobId
    map[LoiConverter.SUBMISSION_COUNT] = mutation.submissionCount

    when (val geometry = mutation.geometry) {
      is Point ->
        map.addGeometryCoordinates(geometry.coordinates.toGeoPoint(), LoiConverter.POINT_TYPE)

      is Polygon ->
        // Holes are excluded since they're not supported in the polygon drawing feature.
        map[LoiConverter.GEOMETRY] = GeometryConverter.toFirestoreMap(geometry).getOrThrow()

      else -> {}
    }

    if (mutation.properties.isNotEmpty()) {
      map[LoiConverter.PROPERTIES] = mutation.properties
    }

    val auditInfo = fromMutationAndUser(mutation, user)
    when (mutation.type) {
      Mutation.Type.CREATE -> {
        map[LoiConverter.CREATED] = auditInfo
        map[LoiConverter.LAST_MODIFIED] = auditInfo
        map[LoiConverter.IS_PREDEFINED] = mutation.isPredefined ?: false
      }

      Mutation.Type.UPDATE -> {
        map[LoiConverter.LAST_MODIFIED] = auditInfo
      }

      Mutation.Type.DELETE,
      Mutation.Type.UNKNOWN -> {
        throw UnsupportedOperationException()
      }
    }
    return map.toPersistentMap()
  }

  private fun MutableMap<String, Any>.addGeometryCoordinates(
    geometryCoordinates: Any,
    geometryType: String,
  ) {
    val geometryMap: MutableMap<String, Any> = HashMap()
    geometryMap[LoiConverter.GEOMETRY_COORDINATES] = geometryCoordinates
    geometryMap[LoiConverter.GEOMETRY_TYPE] = geometryType
    this[LoiConverter.GEOMETRY] = geometryMap
  }

  private fun Coordinates.toGeoPoint() = GeoPoint(lat, lng)
}
