/*
 * Copyright 2024 Google LLC
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
package com.google.android.ground.persistence.remote.firebase.protobuf

import com.google.android.ground.model.User
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.MultiPolygon
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.locationofinterest.LoiProperties
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.proto.LocationOfInterest.Source
import com.google.protobuf.Timestamp
import java.util.Date
import com.google.android.ground.proto.AuditInfo as AuditInfoProto
import com.google.android.ground.proto.Coordinates as CoordinatesProto
import com.google.android.ground.proto.Geometry as GeometryProto
import com.google.android.ground.proto.LinearRing as LinearRingProto
import com.google.android.ground.proto.LocationOfInterest as LocationOfInterestProto
import com.google.android.ground.proto.LocationOfInterest.Property as PropertyProto
import com.google.android.ground.proto.MultiPolygon as MultiPolygonProto
import com.google.android.ground.proto.Point as PointProto
import com.google.android.ground.proto.Polygon as PolygonProto

fun LocationOfInterestMutation.createLoiMessage(user: User): LocationOfInterestProto {
  assert(userId == user.id) { "UserId doesn't match: expected $userId, found ${user.id}" }

  val builder =
    LocationOfInterestProto.newBuilder()
      .setId(locationOfInterestId)
      .setJobId(jobId)
      .setSubmissionCount(submissionCount)
      .setOwnerId(userId)
      .setCustomTag(customId)

  if (properties.isNotEmpty()) {
    builder.putAllProperties(properties.toMessage())
  }

  if (geometry != null) {
    builder.setGeometry(geometry.toMessage())
  }

  val auditInfo = createAuditInfoMessage(user, clientTimestamp, clientTimestamp)

  when (type) {
    Mutation.Type.CREATE -> {
      builder
        .setCreated(auditInfo)
        .setLastModified(auditInfo)
        .setSource(
          if (isPredefined == null) Source.SOURCE_UNSPECIFIED
          else if (isPredefined) Source.FIELD_DATA else Source.IMPORTED
        )
    }
    Mutation.Type.UPDATE -> {
      builder.setLastModified(auditInfo)
    }
    Mutation.Type.DELETE,
    Mutation.Type.UNKNOWN -> {
      throw UnsupportedOperationException()
    }
  }

  return builder.build()
}

private fun createAuditInfoMessage(
  user: User,
  clientTimestamp: Date,
  serverTimestamp: Date,
): AuditInfoProto {
  val builder =
    com.google.android.ground.proto.AuditInfo.newBuilder()
      .setUserId(user.id)
      .setDisplayName(user.displayName)
      .setClientTimestamp(clientTimestamp.toMessage())
      .setServerTimestamp(serverTimestamp.toMessage())
  if (user.photoUrl != null) {
    builder.setPhotoUrl(user.photoUrl)
  }
  return builder.build()
}

private fun Date.toMessage(): Timestamp = Timestamp.newBuilder().setSeconds(time * 1000).build()

private fun Geometry.toMessage(): GeometryProto {
  val geometryBuilder = GeometryProto.newBuilder()
  when (this) {
    is Point -> geometryBuilder.setPoint(toMessage())
    is MultiPolygon -> geometryBuilder.setMultiPolygon(toMessage())
    is Polygon -> geometryBuilder.setPolygon(toMessage())
    is LineString,
    is LinearRing -> throw UnsupportedOperationException("Unsupported type $this")
  }
  return geometryBuilder.build()
}

private fun Coordinates.toMessage(): CoordinatesProto =
  CoordinatesProto.newBuilder().setLatitude(lat).setLongitude(lng).build()

private fun Point.toMessage(): PointProto =
  PointProto.newBuilder().setCoordinates(coordinates.toMessage()).build()

private fun LinearRing.toMessage(): LinearRingProto =
  LinearRingProto.newBuilder().addAllCoordinates(coordinates.map { it.toMessage() }).build()

private fun Polygon.toMessage(): PolygonProto =
  PolygonProto.newBuilder()
    .setShell(shell.toMessage())
    .addAllHoles(holes.map { it.toMessage() })
    .build()

private fun MultiPolygon.toMessage(): MultiPolygonProto =
  MultiPolygonProto.newBuilder().addAllPolygons(polygons.map { it.toMessage() }).build()

private fun LoiProperties.toMessage(): Map<String, PropertyProto> {
  val propertiesBuilder = mutableMapOf<String, PropertyProto>()
  for ((key, value) in this) {
    val property = PropertyProto.newBuilder()
    when (value) {
      is String -> property.setStringValue(value)
      is Number -> property.setNumericValue(value.toDouble())
      else -> throw UnsupportedOperationException("Unknown type $value")
    }
    propertiesBuilder[key] = property.build()
  }
  return propertiesBuilder.toMutableMap()
}
