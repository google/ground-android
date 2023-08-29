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
import com.google.android.ground.model.User
import com.google.android.ground.model.geometry.*
import com.google.android.ground.model.imagery.MbtilesFile
import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskDataMap
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.persistence.local.LocalDataConsistencyException
import com.google.android.ground.persistence.local.room.entity.*
import com.google.android.ground.persistence.local.room.fields.*
import com.google.android.ground.persistence.local.room.relations.JobEntityAndRelations
import com.google.android.ground.persistence.local.room.relations.SurveyEntityAndRelations
import com.google.android.ground.persistence.local.room.relations.TaskEntityAndRelations
import com.google.android.ground.ui.map.Bounds
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.util.*
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import org.json.JSONObject
import timber.log.Timber

fun AuditInfo.toLocalDataStoreObject(): AuditInfoEntity =
  AuditInfoEntity(
    user = UserDetails.fromUser(user),
    clientTimestamp = clientTimestamp.time,
    serverTimestamp = serverTimestamp?.time
  )

fun AuditInfoEntity.toModelObject() =
  AuditInfo(UserDetails.toUser(user), Date(clientTimestamp), serverTimestamp?.let { Date(it) })

private fun TileSource.Type.toLocalDataStoreObject() =
  when (this) {
    TileSource.Type.TILED_WEB_MAP -> TileSourceEntity.TileSourceEntityType.IMAGE
    TileSource.Type.MBTILES_FOOTPRINTS -> TileSourceEntity.TileSourceEntityType.GEOJSON
    TileSource.Type.MOG_COLLECTION -> TileSourceEntity.TileSourceEntityType.MOG
    else -> TileSourceEntity.TileSourceEntityType.UNKNOWN
  }

private fun TileSourceEntity.TileSourceEntityType.toModelObject() =
  when (this) {
    TileSourceEntity.TileSourceEntityType.IMAGE -> TileSource.Type.TILED_WEB_MAP
    TileSourceEntity.TileSourceEntityType.GEOJSON -> TileSource.Type.MBTILES_FOOTPRINTS
    TileSourceEntity.TileSourceEntityType.MOG -> TileSource.Type.MOG_COLLECTION
    else -> TileSource.Type.UNKNOWN
  }

fun TileSource.toLocalDataStoreObject(surveyId: String) =
  TileSourceEntity(surveyId = surveyId, url = url, type = type.toLocalDataStoreObject())

fun TileSourceEntity.toModelObject() = TileSource(url = url, type = type.toModelObject())

fun Geometry.toLocalDataStoreObject() = GeometryWrapper.fromGeometry(this)

fun formatVertices(vertices: List<Point>): String? {
  if (vertices.isEmpty()) {
    return null
  }
  val gson = Gson()
  val verticesArray = vertices.map { (coordinate): Point -> listOf(coordinate.lat, coordinate.lng) }
  return gson.toJson(verticesArray)
}

fun parseVertices(vertices: String?): List<Point> {
  if (vertices.isNullOrEmpty()) {
    return listOf()
  }
  val gson = Gson()
  val verticesArray =
    gson.fromJson<List<List<Double>>>(vertices, object : TypeToken<List<List<Double?>?>?>() {}.type)
  return verticesArray.map { vertex: List<Double> -> Point(Coordinates(vertex[0], vertex[1])) }
}

fun Job.toLocalDataStoreObject(surveyId: String): JobEntity =
  JobEntity(
    id = id,
    surveyId = surveyId,
    name = name,
    suggestLoiTaskType = suggestLoiTaskType?.toString()
  )

fun JobEntityAndRelations.toModelObject(): Job {
  val taskMap = taskEntityAndRelations.map { it.toModelObject() }.associateBy { it.id }
  return Job(
    jobEntity.id,
    jobEntity.name,
    taskMap.toPersistentMap(),
    jobEntity.suggestLoiTaskType?.let { Task.Type.valueOf(it) }
  )
}

fun LocationOfInterest.toLocalDataStoreObject() =
  LocationOfInterestEntity(
    id = id,
    surveyId = surveyId,
    jobId = job.id,
    state = EntityState.DEFAULT,
    caption = caption,
    created = created.toLocalDataStoreObject(),
    lastModified = lastModified.toLocalDataStoreObject(),
    geometry = geometry.toLocalDataStoreObject(),
    submissionCount = submissionCount
  )

fun LocationOfInterestEntity.toModelObject(survey: Survey): LocationOfInterest =
  if (geometry == null) {
    throw LocalDataConsistencyException("No geometry in location of interest $this.id")
  } else {
    LocationOfInterest(
      id = id,
      surveyId = surveyId,
      created = created.toModelObject(),
      lastModified = lastModified.toModelObject(),
      caption = caption,
      geometry = geometry.getGeometry(),
      submissionCount = submissionCount,
      job = survey.getJob(jobId = jobId)
          ?: throw LocalDataConsistencyException(
            "Unknown jobId ${this.jobId} in location of interest ${this.id}"
          )
    )
  }

@Deprecated(
  "Use toLocalDataStoreObject(User) instead",
  ReplaceWith("toLocalDataStoreObject(auditInfo.user)")
)
fun LocationOfInterestMutation.toLocalDataStoreObject(auditInfo: AuditInfo) =
  toLocalDataStoreObject(auditInfo.user)

fun LocationOfInterestMutation.toLocalDataStoreObject(user: User): LocationOfInterestEntity {
  val auditInfo = AuditInfo(user, clientTimestamp).toLocalDataStoreObject()

  return LocationOfInterestEntity(
    id = locationOfInterestId,
    surveyId = surveyId,
    jobId = jobId,
    state = EntityState.DEFAULT,
    caption = caption,
    // TODO(#1562): Preserve creation audit info for UPDATE mutations.
    created = auditInfo,
    lastModified = auditInfo,
    geometry = geometry?.toLocalDataStoreObject(),
    submissionCount = submissionCount
  )
}

fun LocationOfInterestMutation.toLocalDataStoreObject() =
  LocationOfInterestMutationEntity(
    id = id,
    surveyId = surveyId,
    jobId = jobId,
    type = MutationEntityType.fromMutationType(type),
    newGeometry = geometry?.toLocalDataStoreObject(),
    caption = caption,
    userId = userId,
    locationOfInterestId = locationOfInterestId,
    syncStatus = MutationEntitySyncStatus.fromMutationSyncStatus(syncStatus),
    clientTimestamp = clientTimestamp.time,
    lastError = lastError,
    retryCount = retryCount
  )

fun LocationOfInterestMutationEntity.toModelObject() =
  LocationOfInterestMutation(
    id = id,
    surveyId = surveyId,
    jobId = jobId,
    type = type.toMutationType(),
    geometry = newGeometry?.getGeometry(),
    caption = caption,
    userId = userId,
    locationOfInterestId = locationOfInterestId,
    syncStatus = syncStatus.toMutationSyncStatus(),
    clientTimestamp = Date(clientTimestamp),
    lastError = lastError,
    retryCount = retryCount,
  )

fun MultipleChoiceEntity.toModelObject(optionEntities: List<OptionEntity>): MultipleChoice {
  val options = optionEntities.map { it.toModelObject() }
  return MultipleChoice(options.toPersistentList(), this.type.toCardinality())
}

fun MultipleChoice.toLocalDataStoreObject(taskId: String): MultipleChoiceEntity =
  MultipleChoiceEntity(taskId, MultipleChoiceEntityType.fromCardinality(this.cardinality))

private fun OfflineAreaEntityState.toModelObject() =
  when (this) {
    OfflineAreaEntityState.PENDING -> OfflineArea.State.PENDING
    OfflineAreaEntityState.IN_PROGRESS -> OfflineArea.State.IN_PROGRESS
    OfflineAreaEntityState.DOWNLOADED -> OfflineArea.State.DOWNLOADED
    OfflineAreaEntityState.FAILED -> OfflineArea.State.FAILED
    else -> throw IllegalArgumentException("Unknown area state: $this")
  }

private fun OfflineArea.State.toLocalDataStoreObject() =
  when (this) {
    OfflineArea.State.PENDING -> OfflineAreaEntityState.PENDING
    OfflineArea.State.IN_PROGRESS -> OfflineAreaEntityState.IN_PROGRESS
    OfflineArea.State.FAILED -> OfflineAreaEntityState.FAILED
    OfflineArea.State.DOWNLOADED -> OfflineAreaEntityState.DOWNLOADED
  }

fun OfflineArea.toOfflineAreaEntity() =
  OfflineAreaEntity(
    id = this.id,
    state = this.state.toLocalDataStoreObject(),
    name = this.name,
    north = this.bounds.north,
    east = this.bounds.east,
    south = this.bounds.south,
    west = this.bounds.west
  )

fun OfflineAreaEntity.toModelObject(): OfflineArea {
  val northEast = Coordinates(this.north, this.east)
  val southWest = Coordinates(this.south, this.west)
  val bounds = Bounds(southWest, northEast)
  return OfflineArea(this.id, this.state.toModelObject(), bounds, this.name)
}

fun Option.toLocalDataStoreObject(taskId: String) =
  OptionEntity(id = this.id, code = this.code, label = this.label, taskId = taskId)

fun OptionEntity.toModelObject() = Option(id = this.id, code = this.code, label = this.label)

fun SubmissionEntity.toModelObject(loi: LocationOfInterest): Submission {
  val jobId = this.jobId
  val job = loi.job

  if (job.id != jobId) {
    throw LocalDataConsistencyException(
      "LOI job id ${job.id} does not match submission ${this.jobId}"
    )
  }

  return Submission(
    id = this.id,
    surveyId = loi.surveyId,
    locationOfInterest = loi,
    job = job,
    created = this.created.toModelObject(),
    lastModified = this.lastModified.toModelObject(),
    responses = ResponseMapConverter.fromString(job, this.responses)
  )
}

fun Submission.toLocalDataStoreObject() =
  SubmissionEntity(
    id = this.id,
    jobId = this.job.id,
    locationOfInterestId = this.locationOfInterest.id,
    state = EntityState.DEFAULT,
    responses = ResponseMapConverter.toString(this.responses),
    created = this.created.toLocalDataStoreObject(),
    lastModified = this.lastModified.toLocalDataStoreObject(),
  )

fun SubmissionMutation.toLocalDataStoreObject(created: AuditInfo): SubmissionEntity {
  val auditInfo = created.toLocalDataStoreObject()

  return SubmissionEntity(
    id = this.submissionId,
    jobId = this.job!!.id,
    locationOfInterestId = this.locationOfInterestId,
    state = EntityState.DEFAULT,
    responses = ResponseMapConverter.toString(TaskDataMap().copyWithDeltas(this.taskDataDeltas)),
    // TODO(#1562): Preserve creation audit info for UPDATE mutations.
    created = auditInfo,
    lastModified = auditInfo
  )
}

@Throws(LocalDataConsistencyException::class)
fun SubmissionMutationEntity.toModelObject(survey: Survey): SubmissionMutation {
  val job =
    survey.getJob(jobId)
      ?: throw LocalDataConsistencyException("Unknown jobId in submission mutation $id")

  return SubmissionMutation(
    job = job,
    submissionId = submissionId,
    taskDataDeltas = ResponseDeltasConverter.fromString(job, responseDeltas),
    id = id,
    surveyId = surveyId,
    locationOfInterestId = locationOfInterestId,
    type = type.toMutationType(),
    syncStatus = syncStatus.toMutationSyncStatus(),
    retryCount = retryCount,
    lastError = lastError,
    userId = userId,
    clientTimestamp = Date(clientTimestamp)
  )
}

fun SubmissionMutation.toLocalDataStoreObject() =
  SubmissionMutationEntity(
    id = id,
    surveyId = surveyId,
    locationOfInterestId = locationOfInterestId,
    jobId = job!!.id,
    submissionId = submissionId,
    type = MutationEntityType.fromMutationType(type),
    syncStatus = MutationEntitySyncStatus.fromMutationSyncStatus(syncStatus),
    responseDeltas = ResponseDeltasConverter.toString(taskDataDeltas),
    retryCount = retryCount,
    lastError = lastError,
    userId = userId,
    clientTimestamp = clientTimestamp.time
  )

fun SurveyEntityAndRelations.toModelObject(): Survey {
  val jobMap = jobEntityAndRelations.map { it.toModelObject() }.associateBy { it.id }
  val tileSources = tileSourceEntityAndRelations.map { it.toModelObject() }

  return Survey(
    surveyEntity.id,
    surveyEntity.title!!,
    surveyEntity.description!!,
    jobMap.toPersistentMap(),
    tileSources.toPersistentList(),
    surveyEntity.acl?.toStringMap()!!
  )
}

private fun JSONObject.toStringMap(): Map<String, String> {
  val builder = mutableMapOf<String, String>()
  keys().forEach { key: String -> builder[key] = optString(key, toString()) }
  return builder.toPersistentMap()
}

fun Survey.toLocalDataStoreObject() =
  SurveyEntity(
    id = id,
    title = title,
    description = description,
    acl = JSONObject(acl as Map<*, *>)
  )

fun Task.toLocalDataStoreObject(jobId: String?) =
  TaskEntity(
    id = id,
    jobId = jobId,
    index = index,
    label = label,
    isRequired = isRequired,
    taskType = TaskEntityType.fromTaskType(type)
  )

fun TaskEntityAndRelations.toModelObject(): Task {
  var multipleChoice: MultipleChoice? = null

  if (multipleChoiceEntities.isNotEmpty()) {
    if (multipleChoiceEntities.size > 1) {
      Timber.e("More than 1 multiple choice found for task")
    }

    multipleChoice = multipleChoiceEntities[0].toModelObject(optionEntities)
  }

  return Task(
    taskEntity.id,
    taskEntity.index,
    taskEntity.taskType.toTaskType(),
    taskEntity.label!!,
    taskEntity.isRequired,
    multipleChoice
  )
}

private fun TileSetEntityState.toModelObject() =
  when (this) {
    TileSetEntityState.PENDING -> MbtilesFile.DownloadState.PENDING
    TileSetEntityState.IN_PROGRESS -> MbtilesFile.DownloadState.IN_PROGRESS
    TileSetEntityState.DOWNLOADED -> MbtilesFile.DownloadState.DOWNLOADED
    TileSetEntityState.FAILED -> MbtilesFile.DownloadState.FAILED
    else -> throw IllegalArgumentException("Unknown tile source state: $this")
  }

private fun MbtilesFile.DownloadState.toLocalDataStoreObject() =
  when (this) {
    MbtilesFile.DownloadState.PENDING -> TileSetEntityState.PENDING
    MbtilesFile.DownloadState.IN_PROGRESS -> TileSetEntityState.IN_PROGRESS
    MbtilesFile.DownloadState.FAILED -> TileSetEntityState.FAILED
    MbtilesFile.DownloadState.DOWNLOADED -> TileSetEntityState.DOWNLOADED
  }

fun MbtilesFileEntity.toModelObject() =
  MbtilesFile(
    id = id,
    url = url,
    path = path,
    referenceCount = referenceCount,
    downloadState = state.toModelObject()
  )

fun MbtilesFile.toLocalDataStoreObject() =
  MbtilesFileEntity(
    id = id,
    url = url,
    path = path,
    referenceCount = referenceCount,
    state = downloadState.toLocalDataStoreObject()
  )

fun User.toLocalDataStoreObject() =
  UserEntity(id = id, email = email, displayName = displayName, photoUrl = photoUrl)

fun UserEntity.toModelObject() =
  User(id = id, email = email, displayName = displayName, photoUrl = photoUrl)
