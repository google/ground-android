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
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.persistence.remote.firebase.schema.AuditInfoConverter.fromMutationAndUser
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableMap
import com.google.firebase.firestore.GeoPoint

/**
 * Converts between Firestore maps used to merge updates and [LocationOfInterestMutation] instances.
 */
internal object LoiMutationConverter {

  /**
   * Returns a map containing key-value pairs usable by Firestore constructed from the provided
   * mutation.
   */
  fun toMap(mutation: LocationOfInterestMutation, user: User): ImmutableMap<String, Any> {
    val map = ImmutableMap.builder<String, Any>()

    map.put(LoiConverter.JOB_ID, mutation.jobId)

    when (val geometry = mutation.geometry?.getGeometry()) {
      is Point -> map.put(LoiConverter.LOCATION, toGeoPoint(geometry))
      is Polygon -> {
        val geometryMap: MutableMap<String, Any> = HashMap()
        geometryMap[LoiConverter.GEOMETRY_COORDINATES] = toGeoPointList(geometry.shell.vertices)
        geometryMap[LoiConverter.GEOMETRY_TYPE] = LoiConverter.POLYGON_TYPE
        map.put(LoiConverter.GEOMETRY, geometryMap)
      }
      else -> {}
    }

    val auditInfo = fromMutationAndUser(mutation, user)
    when (mutation.type) {
      Mutation.Type.CREATE -> {
        map.put(LoiConverter.CREATED, auditInfo)
        map.put(LoiConverter.LAST_MODIFIED, auditInfo)
      }
      Mutation.Type.UPDATE -> map.put(LoiConverter.LAST_MODIFIED, auditInfo)
      Mutation.Type.DELETE,
      Mutation.Type.UNKNOWN -> throw UnsupportedOperationException()
    }
    return map.build()
  }

  private fun toGeoPoint(point: Point) = GeoPoint(point.coordinate.x, point.coordinate.y)

  private fun toGeoPointList(point: List<Point>): List<GeoPoint> =
    point.map { toGeoPoint(it) }.toImmutableList()
}
