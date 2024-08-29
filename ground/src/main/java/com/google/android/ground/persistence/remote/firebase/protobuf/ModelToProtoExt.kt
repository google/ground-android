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
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.CaptureLocationTaskData
import com.google.android.ground.model.submission.DateTaskData
import com.google.android.ground.model.submission.GeometryTaskData
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.submission.NumberTaskData
import com.google.android.ground.model.submission.PhotoTaskData
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.submission.TimeTaskData
import com.google.android.ground.model.submission.ValueDelta
import com.google.android.ground.model.submission.isNotNullOrEmpty
import com.google.android.ground.model.task.Task
import com.google.android.ground.proto.LinearRing as LinearRingProto
import com.google.android.ground.proto.LocationOfInterest.Property
import com.google.android.ground.proto.LocationOfInterest.Source
import com.google.android.ground.proto.LocationOfInterestKt.property
import com.google.android.ground.proto.MultiPolygon as MultiPolygonProto
import com.google.android.ground.proto.Polygon as PolygonProto
import com.google.android.ground.proto.TaskDataKt.captureLocationResult
import com.google.android.ground.proto.TaskDataKt.dateTimeResponse
import com.google.android.ground.proto.TaskDataKt.drawGeometryResult
import com.google.android.ground.proto.TaskDataKt.multipleChoiceResponses
import com.google.android.ground.proto.TaskDataKt.numberResponse
import com.google.android.ground.proto.TaskDataKt.takePhotoResult
import com.google.android.ground.proto.TaskDataKt.textResponse
import com.google.android.ground.proto.auditInfo
import com.google.android.ground.proto.coordinates
import com.google.android.ground.proto.geometry
import com.google.android.ground.proto.locationOfInterest
import com.google.android.ground.proto.point
import com.google.android.ground.proto.submission
import com.google.android.ground.proto.taskData
import com.google.protobuf.timestamp
import java.util.Date
import kotlinx.collections.immutable.toImmutableMap

// TODO: Add test coverage
fun SubmissionMutation.createSubmissionMessage(user: User) = submission {
  assert(userId == user.id) { "UserId doesn't match: expected $userId, found ${user.id}" }

  val me = this@createSubmissionMessage
  id = submissionId
  loiId = locationOfInterestId
  jobId = job.id
  ownerId = me.userId

  deltas.forEach {
    if (it.newTaskData.isNotNullOrEmpty()) {
      taskData.add(it.toMessage())
    }
  }

  val auditInfo = createAuditInfoMessage(user, clientTimestamp)
  when (type) {
    Mutation.Type.CREATE -> {
      created = auditInfo
      lastModified = auditInfo
    }
    Mutation.Type.UPDATE -> {
      lastModified = auditInfo
    }
    Mutation.Type.DELETE,
    Mutation.Type.UNKNOWN -> {
      throw UnsupportedOperationException()
    }
  }
}

fun LocationOfInterestMutation.createLoiMessage(user: User) = locationOfInterest {
  assert(userId == user.id) { "UserId doesn't match: expected $userId, found ${user.id}" }

  val me = this@createLoiMessage
  id = locationOfInterestId
  jobId = me.jobId
  submissionCount = me.submissionCount
  ownerId = me.userId
  customTag = me.customId

  properties.putAll(me.properties.toMessageMap())

  me.geometry?.toMessage()?.let { geometry = it }

  val auditInfo = createAuditInfoMessage(user, clientTimestamp)

  when (type) {
    Mutation.Type.CREATE -> {
      created = auditInfo
      lastModified = auditInfo
      source =
        if (isPredefined == null) Source.SOURCE_UNSPECIFIED
        else if (isPredefined) Source.IMPORTED else Source.FIELD_DATA
    }
    Mutation.Type.UPDATE -> {
      lastModified = auditInfo
    }
    Mutation.Type.DELETE,
    Mutation.Type.UNKNOWN -> {
      throw UnsupportedOperationException()
    }
  }
}

private fun ValueDelta.toMessage() = taskData {
  val me = this@toMessage
  // TODO: What should be the ID?
  taskId = me.taskId
  // TODO: Add "skipped" field
  when (taskType) {
    Task.Type.TEXT -> textResponse = textResponse { text = (newTaskData as TextTaskData).text }
    Task.Type.NUMBER -> numberResponse = numberResponse {
        number = (newTaskData as NumberTaskData).value
      }
    // TODO: Ensure the dates are always converted to UTC time zone.
    Task.Type.DATE -> dateTimeResponse = dateTimeResponse {
        dateTime = timestamp { seconds = (newTaskData as DateTaskData).date.time / 1000 }
      }
    // TODO: Ensure the dates are always converted to UTC time zone.
    Task.Type.TIME -> dateTimeResponse = dateTimeResponse {
        dateTime = timestamp { seconds = (newTaskData as TimeTaskData).time.time / 1000 }
      }
    Task.Type.MULTIPLE_CHOICE -> multipleChoiceResponses = multipleChoiceResponses {
        (newTaskData as MultipleChoiceTaskData).getSelectedOptionsIdsExceptOther().forEach {
          selectedOptionIds.add(it)
        }
        if (newTaskData.hasOtherText()) {
          otherText = newTaskData.getOtherText()
        }
      }
    Task.Type.DROP_PIN,
    Task.Type.DRAW_AREA -> drawGeometryResult = drawGeometryResult {
        geometry = (newTaskData as GeometryTaskData).geometry.toMessage()
      }
    Task.Type.CAPTURE_LOCATION -> captureLocationResult = captureLocationResult {
        val data = newTaskData as CaptureLocationTaskData
        data.altitude?.let { altitude = it }
        data.accuracy?.let { accuracy = it }
        coordinates = data.location.coordinates.toMessage()
        // TODO: Add timestamp
      }
    Task.Type.PHOTO -> takePhotoResult = takePhotoResult {
        val data = newTaskData as PhotoTaskData
        photoPath = data.remoteFilename
      }
    Task.Type.UNKNOWN -> error("Unknown task type")
  }
}

private fun createAuditInfoMessage(user: User, timestamp: Date) = auditInfo {
  userId = user.id
  displayName = user.displayName
  emailAddress = user.email
  photoUrl = user.photoUrl ?: photoUrl
  clientTimestamp = timestamp.toMessage()
  serverTimestamp = timestamp.toMessage()
}

private fun Date.toMessage() = timestamp { seconds = time / 1000 }

private fun Geometry.toMessage() =
  when (this) {
    is Point -> geometry { point = toMessage() }
    is MultiPolygon -> geometry { multiPolygon = toMessage() }
    is Polygon -> geometry { polygon = toMessage() }
    is LineString,
    is LinearRing -> throw UnsupportedOperationException("Unsupported type $this")
  }

private fun Coordinates.toMessage() = coordinates {
  latitude = lat
  longitude = lng
}

private fun Point.toMessage() = point { coordinates = this@toMessage.coordinates.toMessage() }

private fun LinearRing.toMessage(): LinearRingProto =
  LinearRingProto.newBuilder().addAllCoordinates(coordinates.map { it.toMessage() }).build()

private fun Polygon.toMessage(): PolygonProto =
  PolygonProto.newBuilder()
    .setShell(shell.toMessage())
    .addAllHoles(holes.map { it.toMessage() })
    .build()

private fun MultiPolygon.toMessage(): MultiPolygonProto =
  MultiPolygonProto.newBuilder().addAllPolygons(polygons.map { it.toMessage() }).build()

private fun LoiProperties.toMessageMap(): Map<String, Property> {
  val propertiesBuilder = mutableMapOf<String, Property>()
  for ((key, value) in this) {
    propertiesBuilder[key] =
      when (value) {
        is String -> property { stringValue = value }
        is Number -> property { numericValue = value.toDouble() }
        else -> throw UnsupportedOperationException("Unknown type $value")
      }
  }
  return propertiesBuilder.toImmutableMap()
}
