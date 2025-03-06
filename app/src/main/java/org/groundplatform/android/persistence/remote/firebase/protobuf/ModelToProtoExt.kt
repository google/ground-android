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
package org.groundplatform.android.persistence.remote.firebase.protobuf

import com.google.protobuf.timestamp
import java.util.Date
import kotlinx.collections.immutable.toImmutableMap
import org.groundplatform.android.model.User
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.Geometry
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.MultiPolygon
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.locationofinterest.LoiProperties
import org.groundplatform.android.model.mutation.LocationOfInterestMutation
import org.groundplatform.android.model.mutation.Mutation
import org.groundplatform.android.model.mutation.SubmissionMutation
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.groundplatform.android.model.submission.DateTimeTaskData
import org.groundplatform.android.model.submission.GeometryTaskData
import org.groundplatform.android.model.submission.MultipleChoiceTaskData
import org.groundplatform.android.model.submission.NumberTaskData
import org.groundplatform.android.model.submission.PhotoTaskData
import org.groundplatform.android.model.submission.SkippedTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.TextTaskData
import org.groundplatform.android.proto.LinearRing as LinearRingProto
import org.groundplatform.android.proto.LocationOfInterest.Property
import org.groundplatform.android.proto.LocationOfInterest.Source
import org.groundplatform.android.proto.LocationOfInterestKt.property
import org.groundplatform.android.proto.MultiPolygon as MultiPolygonProto
import org.groundplatform.android.proto.Polygon as PolygonProto
import org.groundplatform.android.proto.TaskDataKt.captureLocationResult
import org.groundplatform.android.proto.TaskDataKt.dateTimeResponse
import org.groundplatform.android.proto.TaskDataKt.drawGeometryResult
import org.groundplatform.android.proto.TaskDataKt.multipleChoiceResponses
import org.groundplatform.android.proto.TaskDataKt.numberResponse
import org.groundplatform.android.proto.TaskDataKt.takePhotoResult
import org.groundplatform.android.proto.TaskDataKt.textResponse
import org.groundplatform.android.proto.auditInfo
import org.groundplatform.android.proto.coordinates
import org.groundplatform.android.proto.geometry
import org.groundplatform.android.proto.locationOfInterest
import org.groundplatform.android.proto.point
import org.groundplatform.android.proto.submission
import org.groundplatform.android.proto.taskData

fun SubmissionMutation.createSubmissionMessage(user: User) = submission {
  assert(userId == user.id) { "UserId doesn't match: expected $userId, found ${user.id}" }

  val me = this@createSubmissionMessage
  id = submissionId
  loiId = locationOfInterestId
  jobId = job.id
  ownerId = me.userId

  deltas.forEach {
    if (it.newTaskData != null) {
      taskData.add(toTaskData(it.taskId, it.newTaskData))
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

private fun toTaskData(id: String, newTaskData: TaskData) = taskData {
  taskId = id

  when (newTaskData) {
    is TextTaskData -> textResponse = textResponse { text = newTaskData.text }
    is NumberTaskData -> numberResponse = numberResponse { number = newTaskData.value }
    is DateTimeTaskData -> dateTimeResponse = dateTimeResponse {
        dateTime = timestamp { seconds = newTaskData.timeInMillis / 1000 }
      }
    is MultipleChoiceTaskData -> multipleChoiceResponses = multipleChoiceResponses {
        newTaskData.getSelectedOptionsIdsExceptOther().forEach { selectedOptionIds.add(it) }
        if (newTaskData.isOtherTextSelected()) {
          otherSelected = true
          otherText = newTaskData.getOtherText()
        }
      }
    is CaptureLocationTaskData -> captureLocationResult = captureLocationResult {
        newTaskData.altitude?.let { altitude = it }
        newTaskData.accuracy?.let { accuracy = it }
        coordinates = newTaskData.location.coordinates.toMessage()
      }
    is GeometryTaskData -> drawGeometryResult = drawGeometryResult {
        geometry = newTaskData.geometry.toMessage()
      }
    is PhotoTaskData -> takePhotoResult = takePhotoResult { photoPath = newTaskData.remoteFilename }
    is SkippedTaskData -> skipped = true
    else -> error("Unknown task type")
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
